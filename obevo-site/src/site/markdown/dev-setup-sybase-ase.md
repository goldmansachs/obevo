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


### Database

https://www.virtualbox.org

Download Ubuntu

Create Hard Disk at least 20GB size

Right Click -> Settings
Under Controller: IDE, remove the existing Empty drive
Add new Optical Drive, then Choose Disk. Choose the Ubuntu ISO

Specify Open SSH server and Samba server

Settings -> Network -> Bridged
Settings -> System -> Processor -> Increase the # of cores to quicken the install

VBoxManage guestproperty get Ubuntu "/VirtualBox/GuestInfo/Net/0/V4/IP"


Add screen shared

choose the icon to select your guest additions
sudo mkdir -p /media/cdrom
sudo mount /dev/cdrom /media/cdrom

cd /media/cdrom
sudo ./VBoxLinuxAdditions.run

restart

mkdir ~/host
sudo mount -t vboxsf virtualboxshared ~/host/


(instructions use /opt/sybase vs /opt/sap)
https://www.petersap.nl/SybaseWiki/index.php?title=Installation_guidelines_ASE_15.7
https://www.petersap.nl/SybaseWiki/index.php?title=Srvbuildres_task_failed


https://www.sap.com/cmp/syb/crm-xu15-int-asexprdm/index.html

sudo su -- root
mkdir -p /opt/sap
mkdir -p /var/sap
groupadd sybase
useradd -g sybase -d /opt/sap sybase
passwd sybase

chown sybase:sybase /opt/sap
chown sybase:sybase /var/sap

su -- sybase
mkdir /opt/sap/install
cd /opt/sap/install

cp ~/host/ase_suite.tar .
tar -xf /home/shantstepanian/host/ase_suite.tar

./setup.bin

Choose defaults except for below:
* Use Express Edition, not Free Developer Edition
http://www.sypron.nl/get_ase_soft.html

Install may pause a while - this is normal
 [==================|==================|==================|==================]
 [------------------|-------------


For Configure New Servers, only enable "Configure new SAP ASE" and "Configure New Job Scheduler" and "Enable Self Management"

SAP ASE Name - whatever you want
Host Name - specify the IP Address

When prompted for the Self Managmeent user name as "sa", hit OK. System administrator's password was set earlier


. /opt/sap/SYBASE.sh

isql64 -Usa -Ppassword -Sxxx.xxx.xxx.xxx:yyyy
or
isql -Usa -Ppassword -Sxxx.xxx.xxx.xxx:yyyy

if
  bash: /opt/sap/OCS-16_0/bin/isql: No such file or directory
