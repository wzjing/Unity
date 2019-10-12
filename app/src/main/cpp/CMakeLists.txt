cmake_minimum_required(VERSION 3.4.0)

add_library(daemon SHARED daemon.cpp)

target_link_libraries(daemon log)