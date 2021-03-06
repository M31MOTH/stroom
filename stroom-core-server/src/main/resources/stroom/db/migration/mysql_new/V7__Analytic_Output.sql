/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

----------------------------------------
-- Analytic Output
----------------------------------------

-- Analytic Output Data Source
CREATE TABLE ANAL_OUT_DAT_SRC(
  ID 				int(11) NOT NULL AUTO_INCREMENT,
  VER				tinyint(4) NOT NULL,
  CRT_DT 			timestamp NULL DEFAULT NULL,
  CRT_USER			varchar(255) DEFAULT NULL,
  UPD_DT 			timestamp NULL DEFAULT NULL,
  UPD_USER 			varchar(255) DEFAULT NULL,
  NAME 				varchar(255) NOT NULL,
  UUID 				varchar(255) DEFAULT NULL,
  DESCRIP 			longtext,
  FK_FOLDER_ID  	int(11) NOT NULL,
  PRIMARY KEY       (ID),
  UNIQUE 			(NAME),
  UNIQUE            (UUID),
  CONSTRAINT 		ANAL_OUT_DAT_SRC_FK_FOLDER_ID FOREIGN KEY (FK_FOLDER_ID) REFERENCES FOLDER (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;