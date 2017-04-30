from flask import Flask, request
from pymongo import MongoClient
import json, requests
from pprint import pprint
from datetime import datetime
app = Flask(__name__)

client = MongoClient()
db = client.solarity
crowdsourcedData = db.crowdsourcedData

class DarkSky:
    def __init__(self, token):
        self.url = 'https://api.darksky.net/'
        self.token = token
    def get_weather(self, latitude, longitude):
        url = '{}forecast/{}/{},{}'.format(self.url, self.token, latitude, longitude)
        return json.loads(requests.get(url).text)
    def get_current_barometric(self, latitude, longitude):  # returns in hPa
        try:
            return self.get_weather(latitude, longitude)['currently']['pressure']
        except KeyError:
            return
    def hPa_to_inHg(self, hPa):
        return 0.0295299830714 * hPa

dark_sky = DarkSky('49cde61a1a23a9691d326d0168bfd5db')
# pprint(dark_sky.get_weather(-37.796484, 144.963279))

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
    for hour in range(0, 24):   # for every hour in the day
        values.append([str(month), str(hour), str(barometric_pressure), '0'])   # TODO: use Dark Sky forecast API for forecasted weather
    data['Inputs']['input1']['Values'] = values
    data['GlobalParameters'] = {}
    data = json.dumps(data)

    response = json.loads(requests.post(url, headers=headers, data=data).text)
    results = [float(entry[4]) for entry in response['Results']['output1']['value']['Values']] # 4th column: predicted solar radiation (W/m2)
    return results

def lux_to_W_per_m2(lux):
    try:
        lux = float(lux)
        return lux * 0.0079
    except ValueError:
        return

@app.route('/', methods=['GET', 'POST'])
def root():
    return 'Hello world'

@app.route('/predictions', methods=['GET'])
def get_predictions():
    month = request.args.get('month')
    latitude = request.args.get('lat')
    longitude = request.args.get('lon')
    measurement = request.args.get('measurement')

    error = None
    if (not month or not latitude or not longitude or not measurement):    # missing params
        error = True
    if error:
        return 'Missing or invalid params.'

    barometric = dark_sky.get_current_barometric(latitude, longitude)
    barometric = dark_sky.hPa_to_inHg(barometric)

    now = datetime.now()
    day = now.day
    month = now.month
    year = now.year
    hour = now.hour
    # TODO: time date from client's locale
    crowdsourcedData.insert_one(\
    {
        "barometricPressure": barometric,
        "day": day,
        "month": month,
        "year": year,
        "hour": hour,
        "radiation": lux_to_W_per_m2(measurement)
    })
    crowdsourcedData.find_one()

    return json.dumps(dict(radiationByHour=get_predicted_radiation(month, barometric)))


# get_predicted_radiation(4, 30)
app.run(host='0.0.0.0', port=5000)
