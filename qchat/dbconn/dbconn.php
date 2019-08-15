<?php

	$DB_HOST="*****";
	$DB_USER="*****";
	$DB_PASSWORD="*****";
	$DB_DBNAME="*****";

	$DBCONN = pg_connect("host=$DB_HOST dbname=$DB_DBNAME user=$DB_USER password=$DB_PASSWORD")
    or die ("DB에 연결할 수 없습니다\n");
?>