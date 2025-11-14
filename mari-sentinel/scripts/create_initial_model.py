#!/usr/bin/env python3
import numpy as np
from sklearn.linear_model import LogisticRegression
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

# 8-dim dummy -> logistic regression
X = np.random.rand(100, 8).astype(np.float32)
y = (X.sum(axis=1) > 4).astype(int)

clf = LogisticRegression()
clf.fit(X, y)

initial_type = [('float_input', FloatTensorType([None, 8]))]
onnx_model = convert_sklearn(clf, initial_types=initial_type, options={"zipmap": False})

with open('initial_model.onnx', 'wb') as f:
    f.write(onnx_model.SerializeToString())
print('initial_model.onnx created')
