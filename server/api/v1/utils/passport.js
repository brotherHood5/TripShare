const passport = require("passport");
const passportJWT = require("passport-jwt");
const ExtractJWT = passportJWT.ExtractJwt;
const JWTStrategy = passportJWT.Strategy;
const User = require("../models/user.model");

var opts = {
  jwtFromRequest: ExtractJWT.fromAuthHeaderAsBearerToken(),
  secretOrKey: process.env.JWT_SECRET,
};

passport.use(
  new JWTStrategy(opts, function verify(jwtPayload, done) {
    return User.findByPk(jwtPayload.id, {
      attributes: { exclude: ["password"] },
    })
      .then((user) => {
        return done(null, user.toJSON());
      })
      .catch((err) => {
        return done(err);
      });
  })
);

const isAuth = passport.authenticate("jwt", { session: false });

module.exports = {
  isAuth,
};
