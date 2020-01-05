import json
import sys
import os

def check_one(filename):
    with open(filename) as f:
        j=json.load(f)

        user=j['userID']
        if j['info']['follower_count']!=len(set(j['followers'])):
            return user+" "+"follower"+' {} {}'.format(j['info']['follower_count'],len(set(j['followers'])))
        if j['info']['following_count']!=len(set(j['followees'])):
            return user+" "+"followee"+' {} {}'.format(j['info']['following_count'],len(set(j['followees'])))
        if j['info']['following_topic_count']!=len(j['topics']):
            return user+" "+"topic"+' {} {}'.format(j['info']['following_topic_count'],len(j['topics']))
        #if j['info']['following_question_count']!=len(j['questions']):
        #    return user+" "+"question"+' {} {}'.format(j['info']['following_question_count'],len(j['questions']))
        return ''
        
for filename in os.listdir('data'):
    try:
        r=check_one('data/'+filename)
        if r:
            print(r)
    except Exception :
        print("exception ",filename)  
    
