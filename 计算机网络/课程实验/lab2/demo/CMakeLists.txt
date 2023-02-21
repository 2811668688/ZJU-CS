cmake_minimum_required(VERSION 3.0)
project(socket_example)


# ./client 127.0.0.1 3333
add_executable(client client.c)
add_executable(client2 client2.c)

add_executable(server_v1 server_v1.c)
add_executable(server_v2 server_v2.c)
add_executable(server_v3 server_v3.c)

add_executable(demo demo.cpp)
target_link_libraries(server_v3 pthread)
target_link_libraries(demo pthread)