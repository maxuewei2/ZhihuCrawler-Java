import json
import sys

with open(sys.argv[1]) as f:
    j=json.load(f)
    info=j['info']
    print(j['userID'])
    print()
    print('follower_count          ',info['follower_count'])
    print('following_count         ',info['following_count'])
    print('following_topic_count   ',info['following_topic_count'])
    print('following_question_count',info['following_question_count'])

    print()

    print('len followers           ',len(j['followers']))
    print('len set followers       ',len(set(j['followers'])))
    print('len followees           ',len(j['followees']))
    print('len set followees       ',len(set(j['followees'])))
    print('len topic               ',len(j['topics']))
    print('len question            ',len(j['questions']))

