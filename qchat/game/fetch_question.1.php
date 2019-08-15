<?php
	/**
	 * ERROR_CODE_0 : 첫 번째 DB 연결 실패
	 * ERROR_CODE_1 : qType 정보 불러오기 실패
	 * ERROR_CODE_2 : 두 번째 DB 연결 실패
	 * ERROR_CODE_3 : 두 번째 결과 비정상
	 */
    include("../dbconn/dbconn.php");
    
    if($_GET['sec_code'] != "thisrequestissentbygameserver!pleasesendmeaquestionasap") exit(-1);

	$query = "SELECT q_table_name FROM q_type WHERE q_type_no=" . $_GET['q_type'];
	$result = pg_exec($DBCONN, $query) or die("ERROR_CODE_0");
	if($result) {
		while($row = pg_fetch_assoc($result)) {
        
			$query2 = "SELECT * FROM " . $row['q_table_name'] . " ORDER BY random() LIMIT 1";
           // $query2 = "SELECT currval('q_sangsik_content_increment') FROM q_sangsik_content";
            //echo "★" . $query2 . "★";

            $result2 = pg_exec($DBCONN, $query2) or die("ERROR_CODE_2");
			if($result2) {
				while($row2 = pg_fetch_assoc($result2)) {
					if($_POST['q_type'] != '4') {
						echo $row2['question'] . ":;:" . $row2['is_ox'] . ":;:" . $row2['answer'] . ":;:0:;:0:;;0:;;0";
					} else {
						echo $row2['question'] . ":;:" . $row2['is_ox'] . ":;:0:;:" . $row2['answer1'] . ":;:" . $row2['answer2'] . ":;:" . $row2['answer3'] . ":;:" . $row['answer4'];
					}
				}
			}
		}
	}
?>