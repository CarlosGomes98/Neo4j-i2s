import pandas
import numpy as np
from sklearn import model_selection
from sklearn.svm import SVC
from sklearn.neural_network import MLPClassifier
from sklearn import preprocessing  
from sklearn.preprocessing import StandardScaler
from toCsv import buildCSV

buildCSV()
scaler = StandardScaler()  
le = preprocessing.LabelEncoder()

dataset = pandas.read_csv("data.csv")
X = dataset.iloc[:, :-1]
Y = dataset.iloc[:, -1]

X = pandas.get_dummies(X)
print(X)
mask = [x % 2 == 0 for x in range(0, X.shape[0])]
print(mask)
oppositeMask = [not x for x in mask]
X_train = X.iloc[mask]
Y_train = Y.iloc[mask]
X_test = X.iloc[oppositeMask]
Y_test = Y.iloc[oppositeMask]
scaler.fit(X_train)

X_train = scaler.transform(X_train)  
X_test = scaler.transform(X_test)  

# models = []
# models.append(("5, 5", MLPClassifier(solver='adam', alpha=1e-5, hidden_layer_sizes=(5, 5), max_iter=1000)))
# models.append(("10, 10", MLPClassifier(solver='adam', alpha=1e-5, hidden_layer_sizes=(10, 10), max_iter=1000)))
# models.append(("10, 10, 5", MLPClassifier(solver='adam', alpha=1e-5, hidden_layer_sizes=(10, 10, 5), max_iter=1000)))
# models.append(("10, 10, 10", MLPClassifier(solver='adam', alpha=1e-5, hidden_layer_sizes=(10, 10, 10), max_iter=1000)))
# models.append(("10, 10, 10, 20, 20, 30", MLPClassifier(solver='adam', alpha=1e-5, hidden_layer_sizes=(10, 10, 10, 20, 20, 30), max_iter=1000))) 
# models.append(("KNN", KNeighborsClassifier(n_neighbors=3)))



# clf = SVC() 
# kfold = model_selection.KFold(n_splits=3, random_state=7)
# cv_results = model_selection.cross_val_score(clf, X_train, Y_train, cv=kfold, scoring="accuracy")
# print(str(cv_results.mean()))

clf = SVC()

clf.fit(X_train, Y_train)
print((clf.predict(X_test) == Y_test).mean()) 
