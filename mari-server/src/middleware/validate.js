// src/middleware/validate.js
const Joi = require('joi');

const validate = ({ body, params, query }, options = { abortEarly: false, allowUnknown: true, stripUnknown: true }) => {
  return (req, res, next) => {
    try {
      if (body) {
        const { error, value } = body.validate(req.body, options);
        if (error) return res.status(400).json({ success: false, error: 'Invalid request body', details: error.details });
        req.body = value;
      }
      if (params) {
        const { error, value } = params.validate(req.params, options);
        if (error) return res.status(400).json({ success: false, error: 'Invalid route params', details: error.details });
        req.params = value;
      }
      if (query) {
        const { error, value } = query.validate(req.query, options);
        if (error) return res.status(400).json({ success: false, error: 'Invalid query params', details: error.details });
        req.query = value;
      }
      next();
    } catch (e) {
      res.status(400).json({ success: false, error: 'Validation error' });
    }
  };
};

module.exports = { validate, Joi };
