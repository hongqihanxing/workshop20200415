
grammar SQLStatement;

import Symbol, Keyword, Literals;

use
    : USE schemaName
    ;
    
schemaName
    : identifier
    ;
    
insert
    : INSERT INTO? tableName columnNames? VALUE assignmentValues
    ;
  
assignmentValues
    : LP_ assignmentValue (COMMA_ assignmentValue)* RP_
    ;

assignmentValue
    : identifier
    ;
    
columnNames
    : LP_ columnName (COMMA_ columnName)* RP_
    ;

columnName
    : identifier
    ;
   
tableName
    : identifier
    ;
    
identifier
    : IDENTIFIER_ | STRING_ | NUMBER_
    ;

select
    : SELECT ASTERISK_? cols?  FROM  table WHERE condition
    ;
condition
    : colName EQ_ colValue
    ;
colName
    : identifier
    ;
colValue
    : identifier
    ;
cols
    : col (COMMA_ col)*
    ;
col
    : identifier
    ;
table
    : identifier
    ;


