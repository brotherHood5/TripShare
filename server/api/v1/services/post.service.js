const Post = require("../models/post.model");
const sequelize = require("../config");

const queryAuthor = {
    association: "author",
    attributes: ["id", "email", "avatar", "user_name", "user_name_non_accent"],
};

const common = (query) => {
    return {
        attributes: {
            exclude: ["created_by"],
        },
        include: [{ ...queryAuthor }],
        ...query,
    };
};

const getAll = async ({ offset, limit, order, no_limit, ...options }) => {
    if (no_limit) {
        limit = null;
        offset = null;
    }

    const posts = await Post.findAll(
        common({
            offset,
            limit,
            ...options,
            order: [["createdAt", order ? order : "DESC"]],
        })
    );

    return posts;
};

const getPostById = async (id) => {
    const post = await Post.findByPk(
        id,
        common({
            include: [
                {
                    ...queryAuthor,
                },
                {
                    association: "schedules",
                    separate: true,
                    include: [
                        {
                            association: "locations",
                            exclude: ["place_id"],
                            through: {},
                        },
                    ],
                    order: [["date", "ASC"]],
                },
            ],
        })
    );

    post.schedules.forEach((schedule) => {
        schedule.locations.sort(
            (a, b) =>
                a.SchedulesLocations.position - b.SchedulesLocations.position
        );
    });

    return post;
};

const createPost = async (post_data) => {
    const newPost = await Post.create(post_data, {
        include: [
            {
                association: "author",
            },
            {
                association: "schedules",
                include: [{ association: "locations", exclude: ["place_id"] }],
            },
        ],
    });
    return await getPostById(newPost.id);
};

const updatePost = async (id, post) => {
    const oldCoverImg = await Post.findByPk(id, { attributes: ["cover_img"] });
    // console.log(oldCoverImg.cover_img);
    const result = await Post.update(
        post,
        common({
            where: { id },
            returning: true,
            individualHooks: true,
        })
    );

    // if (post.cover_img) {
    //   cloudinary.destroy(oldCoverImg.cover_img);
    // }

    return [result[0], result[1][0]];
};

const deletePost = async (id) => {
    return await Post.destroy({ where: { id } });
};

const increaseLikePost = async (id) => {
    return await Post.update(
        { like_count: sequelize.literal("like_count + 1") },
        { where: { id } }
    );
};
const decreaseLikePost = async (id) => {
    return await Post.update(
        { like_count: sequelize.literal("like_count - 1") },
        { where: { id } }
    );
};

const getPostByIdLocation = async (location_id) => {
    console.log(location_id);

    try {
        const post = await Post.findAll(
            common({
                include: [
                    {
                        model: "Schedule",
                        separate: true,
                        include: [
                            {
                                model: "SchedulesLocations",
                                where: { location_id: location_id },
                            },
                        ],
                    },
                    {
                        ...queryAuthor,
                    },
                ],
                where: { is_public: true },
            })
        );
        return post;
    } catch (err) {
        console.log(err);
    }
};
module.exports = {
    getAll,
    getPostById,
    createPost,
    updatePost,
    deletePost,
    increaseLikePost,
    decreaseLikePost,
    getPostByIdLocation,
    model: Post,
};
