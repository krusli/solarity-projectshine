from flask import Flask
import json, requests
from pprint import pprint
app = Flask(__name__)

def get_weather(latitude, longitude):
    url = 'https://api.darksky.net/forecast/49cde61a1a23a9691d326d0168bfd5db/'
    return requests.get(url + latitude + ',' + longitude)

def get_predicted_radiation(month, barometric_pressure):
    url = 'https://ussouthcentral.services.azureml.net/workspaces/587353afdfa84918a76eccfec3717e84/services/652262c1d51b4609bf0ea32001cf7b08/execute?api-version=2.0&details=true'
    headers = {'Authorization':'Bearer pgmzTPb1KW1P43nFKcos7TH8psfQ3Uun+ybyEZwkitR5uCMj5PRBGHzrARNC/aq+4bNXxOzn+MYMvhuQ7JwQgA==', \
                'Content-Type': 'application/json'}

    # build the Azure ML request body
    data = {}
    data['Inputs'] = {}
    data['Inputs']['input1'] = {}
    data['Inputs']['input1']['ColumnNames'] = ['month', 'hour', 'barometricPressure', 'radiation']
    values = []
    for hour in range(1, 25):   # for every hour in the day
        values.append([str(month), str(hour), str(barometric_pressure), '0'])
    data['Inputs']['input1']['Values'] = values
    data['GlobalParameters'] = {}
    data = json.dumps(data)

    response = json.loads(requests.post(url, headers=headers, data=data).text)
    results = [entry[4] for entry in response['Results']['output1']['value']['Values']] # 4th column: predicted solar radiation (W/m2)
    return results

@app.route('/', methods=['GET', 'POST'])
def root():
    return 'Hello world'

get_predicted_radiation(4, 30)
app.run(host='127.0.0.1', port=5000)
