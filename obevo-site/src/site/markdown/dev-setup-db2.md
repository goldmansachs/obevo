<!--

    Copyright 2017 Goldman Sachs.
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->

(WORK IN PROGRESS)


# DB2


sp_configure 'user connections', 100



./db2_install


sudo su -- root
mkdir -p /opt/ibmdb2
mkdir -p /var/ibmdb2
groupadd ibmdb2
useradd -g ibmdb2 -d /opt/ibmdb2 ibmdb2
passwd ibmdb2

chown ibmdb2:ibmdb2 /opt/ibmdb2
chown ibmdb2:ibmdb2 /var/ibmdb2



sudo apt-get install libpam0g:i386
sudo apt-get install libaio1
sudo apt-get install binutils