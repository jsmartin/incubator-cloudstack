# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permission  s and limitations
# under the License.



class devcloud (

  $cs_dir          = $devcloud::params::cs_dir ,
  $devcloud_path   = $devcloud::params::devcloud_path,
  $gitrepo         = $devcloud::params::gitrepo,
  $storage_dir     = $devcloud::params::storage_dir,
  $tomcat_version  = $devcloud::params::tomcat_version,
  $tomcat_url      = $devcloud::params::tomcat_url,
  $tomcat_home     = $devcloud::params::tomcat_home,
  $maven_version  = $devcloud::params::maven_version,
  $maven_url      = $devcloud::params::maven_url,
  $maven_home     = $devcloud::params::maven_home,
  $downloads       = $devcloud::params::downloads,
  $md5sum_local    = $devcloud::params::md5sum_local,
  $md5sum_remote   = $devcloud::params::md5sum_remote,
  $hostuuid        = $::xen_hostuuid,
  $bridge_device_mac = $::macaddress_xenbr0

) inherits devcloud::params {



  Exec { path => [ "/bin/", "/sbin/" , "/usr/bin/", "/usr/sbin/" ] }

  service {
    'ebtables':
      require => Package['ebtables'],
      ensure  => 'running',
      enable  => true;
  'nfs-kernel-server':
      require => Package['nfs-server'],
      ensure => 'running',
      enable => true,
      subscribe => File["/etc/exports"];
  }

  package { [
            "ant",
            "ebtables",
            "iptables",
            "git",
            "mkisofs",
            "mysql-server",
            "nfs-server",
            "openjdk-6-jdk",
            "unzip"
            ]:
      ensure  => latest,

  }

  exec {

    'get_md5sums':
      command => "/usr/bin/wget -N ${md5sum_remote} -O ${md5sum_local}",
      require => File["${storage_dir}/secondary/template/tmpl/1/"],
      timeout => '0';

    'getecho':
      command => "/usr/bin/wget ${devcloud_path}/echo -O /usr/lib/xcp/plugins/echo",
      creates => '/usr/lib/xcp/plugins/echo';

    'setxcpperms':
      command => '/bin/chmod -R 755 /usr/lib/xcp',
      require => Exec['getecho'];

    'get_code':
      command => "git clone ${gitrepo}",
      cwd     => $cs_dir,
      require => [ File["${cs_dir}"]],
      timeout => '7200',
      creates => "$cs_dir/incubator-cloudstack";

    'update_code':
      command => "git pull origin master",
      cwd     => "${cs_dir}/incubator-cloudstack",
      timeout => '7200',
      require => [ Exec["get_code"]];

    "configlocal":
      command => "xe sr-create host-uuid=${hostuuid} name-label=local-storage shared=false type=file device-config:location=${storage_dir}/primary",
      cwd     => '/',
      unless  => '/usr/bin/xe sr-list | /bin/egrep \'local-storage|Cloud Stack Local EXT Storage Pool\'',
      require => [
        File["${storage_dir}/primary"],
        File["/etc/iptables.save"]
        ];

    "configvnc":
      command => 'sed -i \'s/VNCTERM_LISTEN=.\+/VNCTERM_LISTEN="-v 0.0.0.0:1"/\' /usr/lib/xcp/lib/vncterm-wrapper',
      onlyif      => '/bin/grep "0.0.0.0:1" /usr/lib/xcp/lib/vncterm-wrapper';

    'downloadtomcat':
      command => "/usr/bin/wget ${tomcat_url} -P ${cs_dir}/",
      creates => "${cs_dir}/apache-tomcat-${tomcat_version}.zip",
      require => File[$cs_dir],
      timeout => '0';

    "unziptomcat":
      require => [
        Package['unzip'],
        Exec["downloadtomcat"]
        ],
      creates => "${tomcat_home}",
      command => "/usr/bin/unzip apache-tomcat-${tomcat_version}.zip",
      cwd     => "${cs_dir}",
      timeout => '0';

    "downloadmaven":
      command => "/usr/bin/wget ${maven_url} -P ${cs_dir}/",
      creates => "${cs_dir}/apache-maven-${maven_version}-bin.tar.gz",
      require => Exec['unziptomcat'],
      timeout => '0';

	"build_cloudstack":
		require => [
		  Package["ant"],
		  Exec["install_maven"],
		  File["${cs_dir}/incubator-cloudstack/dist"],
      	  File["${cs_dir}/incubator-cloudstack/target"],
		  Package['mkisofs'],
		  File["${cs_dir}/buildcloudstack.sh"]
		  ],
		command => "/opt/cloudstack/buildcloudstack.sh",
		cwd => "/opt/cloudstack/",
    	timeout => '0';


    "install_maven":
      require => Exec["downloadmaven"],
      creates => "${maven_home}",
      command => "/bin/tar xzvf ${cs_dir}/apache-maven-${maven_version}-bin.tar.gz",
      cwd     => "${cs_dir}",
      timeout => '0';

    'tomcatperms':
      command => "chmod +x ${tomcat_home}/bin/*.sh",
      require => Exec["unziptomcat"],;

    "catalina_home":
      require => Exec["unziptomcat"],
      unless  => '/bin/grep CATALINA_HOME /root/.bashrc',
      command => "/bin/echo \"export CATALINA_HOME=${tomcat_home}\" >> /root/.bashrc",
      cwd     => '/';
  "configebtables":
      require => [
        Service['ebtables']
        ],
      command   => "/sbin/ebtables -I FORWARD -d ! $bridge_device_mac -i eth0 -p IPV4 --ip-prot udp --ip-dport 67:68 -j DROP",
      subscribe => Package["ebtables"],
      unless    => "/sbin/ebtables -L | grep \"-I FORWARD -d ! $bridge_device_mac -i eth0 -p IPV4 --ip-prot udp --ip-dport 67:68 -j DROP\"",
      refreshonly => true,
      cwd       => "/",
      path      => "/sbin/:/usr/bin/:/bin"
}


  file {

    [ $cs_dir,
    "${storage_dir}",
    "${storage_dir}/primary",
    "${storage_dir}/secondary",
    "${storage_dir}/secondary/template",
    "${storage_dir}/secondary/template/tmpl",
    "${storage_dir}/secondary/template/tmpl/1",
    "${storage_dir}/secondary/template/tmpl/1/1",
    "${storage_dir}/secondary/template/tmpl/1/5" ]:
    ensure => "directory",
    group  => "0",
    mode   => "755",
    owner => "0";

    [ "${cs_dir}/incubator-cloudstack/dist",
      "${cs_dir}/incubator-cloudstack/target" ] :
      ensure => "directory",
      group  => "0",
      mode   => "755",
      owner => "0",
      require => [ Exec["update_code"]];

	"${cs_dir}/buildcloudstack.sh":
		ensure => 'file',
		mode => '755',
		owner => '0',
		group => '0',
		content => template("devcloud/builddevcloud.sh.erb");

    "/root/.ssh" :
      ensure => "directory",
      group  => "root",
      mode   => "700",
      owner => "root";

    "${cs_dir}/startdevcloud.sh":
      ensure  => 'file',
      source  => 'puppet:///modules/devcloud/startdevcloud.sh',
      mode    => '755',
      owner   => '0',
      group   => '0';
    "/usr/local/bin/compare.sh":
      ensure  => 'file',
      source  => 'puppet:///modules/devcloud/compare.sh',
      mode    => '755',
      owner   => 'root',
      group   => 'root';
	'/etc/iptables.save':
		require => Package['iptables'],
		ensure  => 'file',
		source  => 'puppet:///modules/devcloud/iptables.save',
		group   => '0',
		mode    => '644',
		owner   => '0';
	"/etc/exports":
		require => Package['nfs-server'],
		ensure  => 'file',
		source  => 'puppet:///modules/devcloud/exports',
		mode    => '644',
		owner   => '0',
		group   => '0';

    }


  devcloud::functions::httpdownload{
    $downloads:
      require => [ File["${storage_dir}/secondary/template/tmpl/1/1"],
                   File["${storage_dir}/secondary/template/tmpl/1/5"],
                   Exec["get_md5sums"]
    ]
  }

}



