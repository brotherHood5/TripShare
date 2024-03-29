const dayjs = require("dayjs");

const PostService = require("../services/post.service");
const ScheduleService = require("../services/schedule.service");
const cloudinary = require("../utils/cloudinary");
const Converter = require("../utils/converter");

const OFFSET = 0;
const LIMIT = 10;

const getAll = async (req, res) => {
  try {
    let { page, limit, created_by, no_limit, ...opts } = req.query;
    const query = {
      ...opts,
      where: {
        is_public: true,
      },
      no_limit,
    };
    if (created_by) query.where.created_by = created_by;

    query.limit = limit ? Number(limit) : LIMIT;
    if (!page) page = 1;
    else page = Number(page);
    query.offset = page ? (page - 1) * query.limit : OFFSET;

    const posts = await PostService.getAll(query);
    await Promise.all(
      posts.map(async (post) => {
        post.setDataValue(
          "is_liked_by_you",
          await post.hasLiked_by(req.user.id)
        );
        return post;
      })
    );
    const count = await PostService.model.count({
      where: {
        is_public: true,
      },
    });

    if (no_limit && no_limit === "true") {
      return res.json({
        page: 1,
        nextPage: 1,
        prevPage: 1,
        maxPage: 1,
        limit: count,
        posts: posts,
      });
    }

    const maxPage = Math.ceil(count / query.limit);
    if (page > maxPage || page <= 0) {
      return res.json([]);
    }

    res.json({
      page: page,
      nextPage: page < maxPage ? Number(page) + 1 : maxPage,
      prevPage: page > 1 ? Number(page) - 1 : 1,
      maxPage: maxPage,
      limit: query.limit,
      posts: posts,
    });
  } catch (error) {
    console.error(error);
    res.json(error);
  }
};

const getPostById = async (req, res) => {
  try {
    let post = await PostService.getPostById(req.params.post_id);
    post.setDataValue("is_liked_by_you", await post.hasLiked_by(req.user.id));

    if (post.is_public === false && req.user.id !== post.author.id) {
      return res.status(401).json({
        error: "You can't not access private posts of another user",
      });
    }

    res.json(post);
  } catch (error) {
    res.json(error);
  }
};

const getPostsByUser = async (req, res) => {
  const { user_id } = req.params;
  const { is_public } = req.query;

  if (is_public === "false" && req.user.id !== Number(user_id)) {
    return res.status(401).json({
      error: "You can't not access private posts of another user",
    });
  }

  const query = {
    where: {
      created_by: user_id,
      is_public: is_public ? is_public : true,
    },
    no_limit: true,
  };

  try {
    const posts = await PostService.getAll(query);
    res.json(posts);
  } catch (error) {
    res.json(error);
  }
};

const createSchedules = (start_date, end_date) => {
  const schedules = [];
  const diffDate = end_date.getDate() - start_date.getDate();
  let current_date = new Date(start_date);

  for (let i = 0; i <= diffDate; i++) {
    const datePlan = new Date(current_date);
    schedules.push({
      date: datePlan,
      title: Converter.toScheduleTitle(datePlan),
    });
    current_date.setDate(current_date.getDate() + 1);
  }
  return schedules;
};

const createPost = async (req, res) => {
  try {
    var cover_img = null;

    if (req.file) {
      const upload_img = await Promise.resolve(
        cloudinary.uploadStream(req.file.buffer, {
          folder: "posts/" + req.user.id,
        })
      );
      cover_img = upload_img.url;
    }

    let {
      title,
      brief_description,
      start_date,
      end_date,
      is_public,
      created_by,
    } = req.body;
    if (!created_by) created_by = req.user.id;

    start_date = start_date ? new Date(start_date) : new Date();
    end_date = end_date ? new Date(end_date) : new Date();

    const post = await PostService.createPost({
      title,
      brief_description,
      cover_img,
      start_date,
      end_date,
      is_public,
      created_by,
      schedules: createSchedules(start_date, end_date),
    });

    return res.json(post);
  } catch (error) {
    return res.json(error);
  }
};

const createExamplePost = async (req, res) => {
  let { user_id } = req.query;
  if (!user_id) user_id = req.user.id;
  else user_id = Number(user_id);
  const start_date = dayjs().toDate();
  const end_date = dayjs(start_date).add(3, "day").toDate();
  const postData = {
    title: "Example plan in 3 days",
    brief_description: "This is an example plan in 3 days. Enjoy!",
    start_date,
    end_date,
    is_public: true,
    created_by: user_id,
    schedules: createSchedules(start_date, end_date),
  };

  try {
    const post = await PostService.createPost(postData);
    return res.json(post);
  } catch (error) {
    console.log(error);
    return res.json(error);
  }
};

const increaseView = async (req, res) => {
  const { post_id } = req.body;
  try {
    const post = await PostService.getPostById(post_id);
    await post.increment("view_count", { by: 1 });
    return res.json(post);
  } catch (error) {
    return res.json({ error: error.message });
  }
};

const decreaseView = async (req, res) => {
  const { post_id } = req.body;
  try {
    const post = await PostService.getPostById(post_id);
    if (post.view_count === 0) {
      return res.json(post);
    }

    await post.decrement("view_count", { by: 1 });
    return res.json(post);
  } catch (error) {
    return res.json({ error: error.message });
  }
};

const changeTripDates = async (req, res) => {
  const post_id = req.params.post_id;
  const { start_date, end_date } = req.body;
  try {
    const isUpdated = await ScheduleService.changeScheduleRange(
      post_id,
      start_date,
      end_date
    );
    return res.json([isUpdated, await PostService.getPostById(post_id)]);
  } catch (error) {
    return res.json({ error: error.message });
  }
};

const updatePost = async (req, res) => {
  var cover_img = null;
  if (req.file) {
    const upload_img = await Promise.resolve(
      cloudinary.uploadStream(req.file.buffer, {
        folder: "posts/" + req.user.id,
      })
    );
    cover_img = upload_img.url;
  }

  const { post_id, ...data } = req.body;

  if (cover_img) {
    data.cover_img = cover_img;
  }

  try {
    const post = await PostService.updatePost(post_id, data);
    return res.json(post);
  } catch (error) {
    return res.json(error);
  }
};

const deletePost = async (req, res) => {
  const { post_id } = req.body;
  const post = await PostService.getPostById(post_id);
  if (
    post.cover_img &&
    post.cover_img !==
      "https://res.cloudinary.com/dkzlalahi/image/upload/q_90/v1681707145/default_trip_plan_cover_img.png"
  ) {
    cloudinary.destroy(post.cover_img);
    console.log("delete cover img");
  }

  try {
    const result = await PostService.deletePost(post_id);

    return res.json(result);
  } catch (error) {
    return res.json(error);
  }
};

const getPostByIdLocation = async (req, res) => {
  try {
    const { location_id } = req.params;
    if (!location_id) {
      return res.json(400).json({ error: "location_id is required" });
    }

    let posts = await PostService.getPostByIdLocation(location_id);
    await Promise.all(
      posts.map(async (post) => {
        post.setDataValue(
          "is_liked_by_you",
          await post.hasLiked_by(req.user.id)
        );
        return post;
      })
    );
    res.json(posts);
  } catch (err) {
    res.json(err);
  }
};

module.exports = {
  getAll,
  getPostById,
  getPostsByUser,
  createPost,
  createExamplePost,
  changeTripDates,
  updatePost,
  deletePost,
  getPostByIdLocation,
  increaseView,
  decreaseView,
};
