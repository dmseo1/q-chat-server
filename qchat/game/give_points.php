<?php
	/**
	 * ERROR_CODE_0 : 첫 번째 DB 연결 실패
	 * ERROR_CODE_1 : qType 정보 불러오기 실패
	 * ERROR_CODE_2 : 두 번째 DB 연결 실패
	 * ERROR_CODE_3 : 두 번째 결과 비정상
	 */
	include("../dbconn/dbconn.php");
	
    if($_GET['sec_code'] != "thisrequestissentbygameserver!pleasegivepointstomyroommember") exit(-1);

	$user_list = explode(":;:", $_GET['user_list']);
	for($i = 0; $i < count($user_list); $i++) {
       
        $user_points = explode("::", $user_list[$i]);
        if($user_points[0] == "") break;
		$query = "UPDATE users SET user_point = user_point + " . $user_points[1] . ", user_exp = user_exp + " . $user_points[1] . " WHERE user_no=" . $user_points[0];
		$result = pg_exec($DBCONN, $query) or die("ERROR_CODE_0");
	}

	echo "complete";
?>