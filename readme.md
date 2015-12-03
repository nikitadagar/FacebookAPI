## Distributed Operating Systems 
### Project 4.1 - Facebook API 

**Members:**
- Alok Sharma (40838147)
- Nikita Dagar (10634310)

*—————————————————————*
###Server
*—————————————————————*

For the server, we have implemented the POST, GET, and DELETE HTTP requests for Page, Post, User Profile, Friends List, Photos and Albums.
You can ping the server at localhost:5000 with the following requests:

1. POST User:

POST /user HTTP/1.1
Host: localhost:5000
Content-Type: application/json
Content-Length: 203
Cache-Control: no-cache

{"email": "nikita@nikita.com", "firstname" : "Nikita", "lastname" : "Dagar", "gender" : "female"}

2. GET User:

GET /user/1 HTTP/1.1
Host: localhost:5000
Content-Type: application/json
Content-Length: 203
Cache-Control: no-cache

3. Delete User:

DELETE /user/1 HTTP/1.1
Host: localhost:5000
Content-Type: application/json
Content-Length: 203
Cache-Control: no-cache

All other requests are illustrated below with only the URLs

4. POST Page : http://localhost:5000/page        (details sent in a JSON along with the request)
5. GET Page : http://localhost:5000/page/id
6. DELETE Page : http://localhost:5000/page/id

7. POST Post : http://localhost:5000/post        (details sent in a JSON along with the request)
8. GET Post : http://localhost:5000/post/id
9. DELETE Post : http://localhost:5000/post/id

10. POST User : http://localhost:5000/user        (details sent in a JSON along with the request)
11. GET User : http://localhost:5000/user/id
12. DELETE User : http://localhost:5000/user/id

13. POST Page : http://localhost:5000/friendsList        (details sent in a JSON along with the request)
14. GET Page : http://localhost:5000/friendsList/id

15. POST Photo : http://localhost:5000/photo        (details sent in a JSON along with the request)
16. GET Photo : http://localhost:5000/photo/id
17. DELETE Photo : http://localhost:5000/photo/id

18. POST Album : http://localhost:5000/album        (details sent in a JSON along with the request)
19. GET Album : http://localhost:5000/album/id
20. DELETE Album : http://localhost:5000/album/id


———————————————————————————————
Client
———————————————————————————————

The client is an actor which is a simulator of a real life user. It carries out the common actions by a user on Facebook like Add a post, Upload a photo, get all photo albums, add a friend etc. The number of total user simultaneously tested were 1000. 



