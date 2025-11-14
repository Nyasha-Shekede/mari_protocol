// src/controllers/authController.js
const User = require('../models/User');
const jwt = require('jsonwebtoken');
const config = require('../config');

class AuthController {
  async register(req, res) {
    try {
      let { username, password, bioHash, phoneNumber } = req.body;

      // Accept minimal payload (username, phoneNumber) and synthesize missing fields.
      if (username && phoneNumber && (!password || !bioHash)) {
        password = password || `pass-${phoneNumber}`;
        bioHash = bioHash || `bio-${phoneNumber}`;
      }

      // Check if user exists (by username OR phoneNumber)
      const existingUser = await User.findOne({ $or: [{ username }, { phoneNumber }] });
      if (existingUser) {
        return res.status(409).json({
          success: false,
          error: 'User already exists'
        });
      }

      // Create new user
      const user = new User({ username, password, bioHash, phoneNumber });

      await user.save();

      // Generate token
      const token = user.generateAuthToken();

      res.status(201).json({
        success: true,
        data: {
          user: {
            id: user._id,
            username: user.username,
            role: user.role
          },
          token
        }
      });
    } catch (error) {
      res.status(400).json({
        success: false,
        error: error.message
      });
    }
  }

  async login(req, res) {
    try {
      const { username, phoneNumber, password } = req.body;

      // Find user by username or phoneNumber
      const query = username ? { username } : (phoneNumber ? { phoneNumber } : null);
      if (!query) {
        return res.status(400).json({ success: false, error: 'username or phoneNumber and password are required' });
      }
      const user = await User.findOne(query);
      if (!user) {
        return res.status(401).json({
          success: false,
          error: 'Invalid credentials'
        });
      }

      // Check password
      const isValidPassword = await user.comparePassword(password);
      if (!isValidPassword) {
        return res.status(401).json({
          success: false,
          error: 'Invalid credentials'
        });
      }

      // Generate token
      const token = user.generateAuthToken();

      res.json({
        success: true,
        data: {
          user: {
            id: user._id,
            username: user.username,
            role: user.role
          },
          token
        }
      });
    } catch (error) {
      res.status(400).json({
        success: false,
        error: error.message
      });
    }
  }

  async profile(req, res) {
    try {
      const user = await User.findById(req.user.userId).select('-password');
      res.json({
        success: true,
        data: user
      });
    } catch (error) {
      res.status(400).json({
        success: false,
        error: error.message
      });
    }
  }
}

module.exports = new AuthController();
