<?php
	include("../dbconn/dbconn.php");
	$query = "SELECT preset from room_title_preset";
	$result = pg_exec($DBCONN, $query) or die("ERROR_CODE_0");
	if($result) {
		$title_presets = array();
		while($row = pg_fetch_assoc($result)) {
			array_push($title_presets,
				$row['preset']);
		}
		echo $title_presets[mt_rand(0, count($title_presets) - 1)];
	} else {
		echo "ERROR_CODE_1";
	}
?>