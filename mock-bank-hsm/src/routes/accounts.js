const express = require('express');
const router = express.Router();
const AccountController = require('../controllers/accountController');
const { validate, Joi } = require('../middleware/validate');

router.post('/', validate({ body: Joi.object({
  name: Joi.string().required(),
  mariBioHash: Joi.string().optional(),
  initialBalance: Joi.number().min(0).optional()
}) }), AccountController.createAccount);

router.get('/:id', validate({ params: Joi.object({ id: Joi.string().required() }) }), AccountController.getAccount);

router.post('/reserve', validate({ body: Joi.object({
  bioHash: Joi.string().required(),
  initialReserve: Joi.number().min(0).optional(),
  userId: Joi.string().optional()
}) }), AccountController.createReserve);

router.get('/reserve/:bioHash', validate({ params: Joi.object({ bioHash: Joi.string().required() }) }), AccountController.getReserve);

module.exports = router;
