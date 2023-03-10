const router = require("express").Router();
const upload = require("../utils/multer");

const PostController = require("../controllers/post.controller");

router.get("/", PostController.getAll);

router.post("/create", upload.single("cover_img"), PostController.createPost);

router.patch("/update", PostController.updatePost);

router.delete("/delete", PostController.deletePost);

module.exports = router;
