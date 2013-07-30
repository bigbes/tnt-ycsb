# Tarantool

## Introduction

Tarantool is NoSQL In-Memory Key-Value DB, which is widely used in mail.ru.
It's opensource, hosted on [github][tnt-github].
Tarantool supports number of features:

* Defferent types of index: HASH (Faster than TREE, but less functionality), TREE and BITSET (Bit operations on values in indexes)
* Multipart Keys for both HASH and TREE's
* Data persistence is avhieved by Write Ahead Log(WAL) and snapshots.
* Supports asynchronous replication, hot standby.
* Uses coroutines and async.IO to implement high-performance lock-free access to data.
* Stored procedures in Lua (Using LuaJIT)
* Supports plugins written on C/C++ (Have two basic plugins for working with MySQL and PostgreSQL)
* Have support of memcached protocol

## Quick start

This section descrives how to run YCSB on Tarantool running locally.

### 1. Start Tarantool

First, clone Tarantool from it's own git repo and build it (You'll need cmake >= 2.6 and gcc ~>= 4.4):

	git clone git@github.com:tarantool/tarantool.git --recursive
	cd tarantool
	cmake .
	make
	cp YCSB/tarantool/config/tarantool-tree.cfg <vardir>
	cp TNT/src/box/tarantool_box <vardir>
	cd <vardir>
	./tarantool_box --init-storage
	./tarantool_box &

OR you can simply download ans install binary package for your GNU/Linux or BSD distro from http://tarantool.org/download.html

### 2. Set Up YCSB

Clone the YCSB git repository and compile:

    git clone git://github.com/brianfrankcooper/YCSB.git
    cd YCSB
    mvn clean package

### 3. Run YCSB
    
Now you are ready to run! First, load the data:

    ./bin/ycsb load tarantool -s -P workloads/workloada

Then, run the workload:

    ./bin/ycsb run tarantool -s -P workloads/workloada

See the next section for the list of configuration parameters for Tarantool.

## Tarantool Configuration Parameters

#### 'tnt.host' (default : 'localhost')
Which host YCSB must use for connection with Tarantool
#### 'tnt.port' (default : 33013)
Which port YCSB must use for connection with Tarantool
#### 'tnt.space' (default : 0) 
    (possible values: 0 .. 255)
Which space YCSB must use for benchmark Tarantool
#### 'tnt.call' (default : false) 
    (possible values: false, true)
If tnt.call is set to True - you may benchmark Tarantool Lua bindings,
instead of Tarantool basic Protocol.

[tnt-github]:https://github.com/tarantool/tarantool/
