const Rating = require("../models/rating.model");
const Post = require("../models/post.model");
const { Op } = require("sequelize");

const createRating = async (ratingData) => {
  try {
    const rating = await Rating.create(ratingData);
    const post = await Post.findByPk(ratingData.post_id);
    await post.increment("rating_count", { by: 1 });
    post.avg_rating =
      (post.avg_rating * (post.rating_count - 1) + ratingData.score) /
      post.rating_count;
    await post.save();
    return { success: 1, error: null, rating };
  } catch (error) {
    console.log(error);
    return { success: 0, error: "Can't create ratting" };
  }
};

const getAllRating = async (post_id, options) => {
  let rating = await Rating.findAndCountAll({
    where: {
      post_id: {
        [Op.eq]: post_id,
      },
    },
    attributes: {
      exclude: ["post_id"],
    },
    order: [["createdAt", "DESC"]],
    ...options,
  });
  return rating;
};
module.exports = {
  createRating,
  getAllRating,
};
