import express from "express";
import { passport } from "../config/passport";
import { app, userList } from "../main";
import jwt from "jsonwebtoken";
import { User } from "../classes/user";

export const router = express.Router();

router.post("/login", (req, res) => {
	passport.authenticate("local", { session: false }, (err, user) => {
		if (err || !user) {
			return res.status(400).json({
				message: err.message
			});
		}
		req.login(user, { session: false }, (err) => {
			if (err) {
				res.send(err);
			}

			// modify secret
			console.log("Signing token with secret: " + app.get("jwt-secret"));
			const token = jwt.sign({ id: user.id }, app.get("jwt-secret"), { expiresIn: "5h" });
			return res.json({ token });
		});
		return false;
	})(req, res);
});

// Add new user
router.post("/register", (req, res) => {
	console.log("POST /users");
	const user = new User(userList.counter, req.body.username, req.body.password);
	console.log("yo " + ((user as User) instanceof User));
	userList.addUser(user);
	console.log(userList);
	res.end(JSON.stringify(user));
});
