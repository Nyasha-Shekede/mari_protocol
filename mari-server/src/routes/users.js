// src/routes/users.js
const express = require('express');
const router = express.Router();
const User = require('../models/User');
const { authMiddleware } = require('../middleware/auth');

router.get('/profile', authMiddleware, async (req, res) => {
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
});

router.put('/location', authMiddleware, async (req, res) => {
  try {
    const { latitude, longitude, grid } = req.body;
    
    const user = await User.findByIdAndUpdate(
      req.user.userId,
      { 
        lastLocation: { latitude, longitude, grid },
        updatedAt: new Date()
      },
      { new: true }
    ).select('-password');

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
});

// Lightweight lookup by phone number or username
router.get('/lookup', async (req, res) => {
  try {
    const { phone, username } = req.query;
    if (!phone && !username) {
      return res.status(400).json({ success: false, error: 'phone or username is required' });
    }
    const query = phone ? { phoneNumber: phone } : { username };
    // Do not select or expose email in lightweight directory lookup
    const user = await User.findOne(query).select('username phoneNumber');
    if (!user) return res.status(404).json({ success: false, error: 'Not found' });
    res.json({ success: true, data: { id: String(user._id), username: user.username, phoneNumber: user.phoneNumber } });
  } catch (error) {
    res.status(400).json({ success: false, error: error.message });
  }
});

module.exports = router;
