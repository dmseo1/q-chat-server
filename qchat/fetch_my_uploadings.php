<?php
	include("dbconn.php");
	$email = $_POST['email'];
	$query = "SELECT * from dongmin.videos where uploader='$email' and is_removed_from_uploadinglist=0 and is_removed=0";
	$result = pg_exec($DBCONN, $query) or die("ERROR");
	$data = array();
	if($result) {
		while($row = pg_fetch_assoc($result)) {
			array_push($data,
				array('id' => $row['id'],
				'title' => $row['title'],
				'thumbnail_path' => $row['thumbnail_path'],
				'filesize' => $row['filesize'],
				'is_upload_complete' => $row['is_upload_complete']
				));
		}
		header('Content-Type: application/json; charset=utf8');
		$json = json_encode(array("uploadinglist"=>$data));
		echo $json;
	} else {
		echo "ERROR";
	}
?>