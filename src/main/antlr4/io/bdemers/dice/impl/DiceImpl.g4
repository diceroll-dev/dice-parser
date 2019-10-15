grammar DiceImpl ;

expression
    :   nDiceFace # nDice
    |   keepDice # keep
    |   diceFace # dice
    |   diceFace X # d10x // d10x
    |   numberOfDice=INT? D FUDGE #FudgeDice // 2dF
    |   numberOfDice=INT? D FUDGE DOT weight=INT # nDotFudge // 2dF.1
    |   compound # CompoundDice // 3d6!!
    |   compound (EQUAL | LESS_THEN_EQUAL | GREATER_THEN_EQUAL)? right=expression # opCompoundDice// 3d6!!>5 or 3d6!!5
    |   explode #ExplodDice // 3d6!
    |   explode (EQUAL | LESS_THEN_EQUAL | GREATER_THEN_EQUAL)? right=expression # nExplodeDice// 3d6!>5 or 3d6!5
    |   targetPool # target
    |   left=expression diceFace #ExpressionDice // (2+4)d6
    |   LPAREN expression RPAREN #Parenthesis // (2)
    |   left=expression LPAREN right=expression RPAREN # CombineParenthesis // multiply 2(6)
    |   left=expression MUL right=expression # Multiply // 2 * 3
    |   left=expression DIV right=expression # Divide // 2 / 3
    |   left=expression ADD right=expression # Add // 2 + 3
    |   left=expression SUB right=expression # Subtract // 2 - 3
    |   INT #Int ;

diceFace // d6
   : D numberOfSides=INT ;

nDiceFace // 2d6
   : numberOfDice=INT diceFace ;

targetPool
   // 4d10>6
   : nDiceFace (EQUAL | LESS_THEN_EQUAL | GREATER_THEN_EQUAL) targetNumber=INT
   // (4d10+2)>6
   | LPAREN nDiceFace (ADD | SUB) modifier=INT RPAREN (EQUAL | LESS_THEN_EQUAL | GREATER_THEN_EQUAL) targetNumber=INT ;

keepDice
   // 4d6k2
   : nDiceFace KEEP numberOfDice=INT
   // 4d6(k2)
   | nDiceFace LPAREN KEEP numberOfDice=INT RPAREN ;

explode
   : numberOfDice=INT? diceFace BANG ;

compound
    : numberOfDice=INT? diceFace BANG BANG;

D   : ( 'd' | 'D' ) ;
X   : ( 'x' | 'X' ) ;
FUDGE  : ( 'f' | 'F' ) ;
KEEP   : ( 'k' | 'K' ) ;
DOT : '.' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;
LPAREN : '(' ;
RPAREN : ')' ;
LESS_THEN_EQUAL : '<' ;
GREATER_THEN_EQUAL : '>' ;
EQUAL : '=' ;
BANG : '!' ;
INT : [0-9]+ ;
WS  : [ \t\r\n\f]+ -> skip ;
