import json
import sys
import os
        
def check_one(filename):
    with open(filename) as f:
        j=json.load(f)

        user=j['userID']
        info=j['info']
        
        a,b=info['follower_count'],len(set(j['followers']))
        if a!=b:
            return '{} follower {} {}'.format(user,a,b)
        
        a,b=info['following_count'],len(set(j['followees']))
        if a!=b:
            return '{} followee {} {}'.format(user,a,b)
        
        a,b=info['following_topic_count'],len(j['topics'])
        if a!=b:
            return '{} topic    {} {}'.format(user,a,b)
        
        #a,b=info['following_question_count'],len(j['questions'])
        #if a!=b:
        #    return '{} question {} {}'.format(user,a,b)
        return ''
        
files=os.listdir('data')
files.remove('.empty')        
for filename in files:
    
    try:
        r=check_one('data/'+filename)
        if r:
            print(r)
    except Exception :
        print(filename, "json parsing error ")  
    
