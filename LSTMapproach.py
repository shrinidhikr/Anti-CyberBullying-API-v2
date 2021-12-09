from flask import Flask
from flask_restful import reqparse,Api,Resource 
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

parser=reqparse.RequestParser()
parser.add_argument('query')

#from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder

from keras.optimizers import RMSprop
from keras.preprocessing.text import Tokenizer
from keras.preprocessing import sequence
from keras.callbacks import EarlyStopping
import pickle
import RNN

class Spam(Resource):
    def get(self):
        try:
            file=open("model.pickle","rb")
            model=pickle.load(file)
            file.close()
        except:
            df = pd.read_csv("train.csv")
            dft=pd.read_csv("test_with_solutions.csv")
            df.head()
            dft.head()
            df.drop(['Date'],axis=1,inplace=True)
            dft.drop(['Date','Usage'],axis=1,inplace=True)
            
            sns.countplot(df.Insult)
            plt.xlabel('Label')
            plt.title('Number of non-bully vs bully messages in trianing dataset')
            
            X = df.Comment
            Y = df.Insult
            le = LabelEncoder()
            Y = le.fit_transform(Y)
            Y = Y.reshape(-1,1)
            le = LabelEncoder()
            
            #X_train,X_test,Y_train,Y_test = train_test_split(X,Y,test_size=0.15)
            X_train,Y_train=X,Y
            max_words = 1000
            max_len = 100
            tok = Tokenizer(num_words=max_words)
            tok.fit_on_texts(X_train)
            pickle.dump(tok,open("tok.pickle","wb"))
            sequences = tok.texts_to_sequences(X_train)
            sequences_matrix = sequence.pad_sequences(sequences,maxlen=max_len)
            
            model = RNN.RNN()
            model.summary()
            model.compile(loss='binary_crossentropy',optimizer=RMSprop(),metrics=['accuracy'])
            
            model.fit(sequences_matrix,Y_train,batch_size=128,epochs=10,
                      validation_split=0.2,callbacks=[EarlyStopping(monitor='val_loss',min_delta=0.0001)])
            f=open("model.pickle","wb")
            pickle.dump(model,f)
            f.close()
        max_words = 1000
        max_len = 100
        args=parser.parse_args()
        X_test=""
        X_test=[args['query']]
        tok = pickle.load(open("tok.pickle","rb"))
        test_sequences = tok.texts_to_sequences(X_test)
        test_sequences_matrix = sequence.pad_sequences(test_sequences,maxlen=max_len)
        ans=model.predict(test_sequences_matrix,batch_size=None,verbose=0,steps=None)
        return("".join(str(ans[0][0])))


app = Flask(__name__)
api = Api(app)
api.add_resource(Spam, '/')

if __name__=="__main__":
 app.run(debug=True)
