const Post = require("../models/post.model");
const User = require("../models/user.model");
const { Op } = require("sequelize");

const commonPost = {
  attributes: { exclude: ["created_by", "title_non_accent"] },
};

const commonUser = {
  attributes: ["id", "user_name", "user_name_non_accent", "email", "avatar"],
};

const searchPost = async (query) => {
  const result = await Post.findAll({
    ...commonPost,
    where: {
      [Op.and]: [
        Post.sequelize.literal("_search @@ plainto_tsquery('english', :query)"),
        { is_public: true },
      ],
    },

    replacements: { query },
  });
  return result;
};

const searchUser = async (query) => {
  const result = await User.findAll({
    ...commonUser,
    where: {
      [Op.or]: [
        User.sequelize.literal("_search @@ plainto_tsquery('english', :query)"),
        {
          [Op.or]: [
            User.sequelize.where(
              User.sequelize.fn("lower", User.sequelize.col("user_name")),
              Op.like,
              User.sequelize.fn("lower", `%${query}%`)
            ),
            User.sequelize.where(
              User.sequelize.fn(
                "lower",
                User.sequelize.col("user_name_non_accent")
              ),
              Op.like,
              User.sequelize.fn("lower", `%${query}%`)
            ),
          ],
        },
      ],
    },
    replacements: { query },
  });
  return result;
};

module.exports = {
  searchPost,
  searchUser,
};
