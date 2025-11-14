# Mari Core Server API Documentation

## Authentication

### Register User
`POST /api/auth/register`

Body:
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "bioHash": "string",
  "phoneNumber": "string"
}
```

### Login
`POST /api/auth/login`

Body:
```json
{
  "email": "string",
  "password": "string"
}
```

## Transactions

### Create Transaction
`POST /api/transactions`

Body:
```json
{
  "senderBioHash": "string",
  "receiverBioHash": "string",
  "amount": 100.00,
  "locationGrid": "grid123",
  "coupon": "exp=1234567890&g=grid123&s=0.123&b=bio123",
  "physicsData": {
    "location": {
      "latitude": 40.7128,
      "longitude": -74.0060,
      "grid": "grid123"
    },
    "motion": {
      "x": 0.1,
      "y": 0.2,
      "z": 0.3
    },
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### Get Transaction
`GET /api/transactions/:transactionId`

### List Transactions
`GET /api/transactions?bioHash=xxx&status=PENDING&limit=50&page=1`

## Settlement

### Process Settlement Batch
`POST /api/settlement/process`

Body:
```json
{
  "batchId": "batch123",
  "merchantId": "merchant123",
  "transactions": [
    {
      "id": "txn123",
      "amount": 100.00,
      "coupon": "exp=1234567890&g=grid123&s=0.123&b=bio123",
      "physicsData": {
        "location": { "latitude": 40.7128, "longitude": -74.0060, "grid": "grid123" },
        "motion": { "x": 0.1, "y": 0.2, "z": 0.3 },
        "timestamp": "2024-01-01T00:00:00Z"
      }
    }
  ],
  "seal": "batch-seal-123"
}
```

## SMS Webhooks

### Incoming SMS
`POST /webhook/sms/incoming`

Body:
```json
{
  "from": "+1234567890",
  "to": "+0987654321",
  "body": "MARI_SMS:eyJhbW91bnQiOjEwMC4wLCJzZW5kZXIiOiJiaW8xMjMifQ==",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### SMS Status Update
`POST /webhook/sms/status`

Body:
```json
{
  "messageId": "msg123",
  "status": "delivered"
}
```
