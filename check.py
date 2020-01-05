import json
import sys

with open(sys.argv[1]) as f:
    j=json.load(f)

    print(j['userID'])
    print()
    print('follower_count          ',j['info']['follower_count'])
    print('following_count         ',j['info']['following_count'])
    print('following_topic_count   ',j['info']['following_topic_count'])
    print('following_question_count',j['info']['following_question_count'])

    print()

    print('len followers           ',len(j['followers']))
    print('len set followers       ',len(set(j['followers'])))
    #print(sorted(set(j['followers'])))
    print('len followees           ',len(set(j['followees'])))
    print('len topic               ',len(j['topics']))
    print('len question            ',len(j['questions']))
#    print(j['followers'][0]['url_token'])
#    print(j['followees'][0]['url_token'])
#    print(j['topic'][0]['topic'])
    
