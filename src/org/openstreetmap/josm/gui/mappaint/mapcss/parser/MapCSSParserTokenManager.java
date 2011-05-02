/* Generated By:JavaCC: Do not edit this line. MapCSSParserTokenManager.java */
package org.openstreetmap.josm.gui.mappaint.mapcss.parser;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context;
import org.openstreetmap.josm.gui.mappaint.mapcss.Expression;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.mappaint.mapcss.Expression.FunctionExpression;
import org.openstreetmap.josm.gui.mappaint.mapcss.Expression.LiteralExpression;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSException;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.ChildOrParentSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.LinkSelector;
import org.openstreetmap.josm.tools.Pair;

/** Token Manager. */
public class MapCSSParserTokenManager implements MapCSSParserConstants
{

  /** Debug output. */
  public  java.io.PrintStream debugStream = System.out;
  /** Set debug output. */
  public  void setDebugStream(java.io.PrintStream ds) { debugStream = ds; }
private final int jjStopStringLiteralDfa_0(int pos, long active0)
{
   switch (pos)
   {
      case 0:
         if ((active0 & 0x2000000800L) != 0L)
            return 14;
         return -1;
      default :
         return -1;
   }
}
private final int jjStartNfa_0(int pos, long active0)
{
   return jjMoveNfa_0(jjStopStringLiteralDfa_0(pos, active0), pos + 1);
}
private int jjStopAtPos(int pos, int kind)
{
   jjmatchedKind = kind;
   jjmatchedPos = pos;
   return pos + 1;
}
private int jjMoveStringLiteralDfa0_0()
{
   switch(curChar)
   {
      case 33:
         return jjStopAtPos(0, 23);
      case 36:
         return jjStopAtPos(0, 35);
      case 38:
         return jjStopAtPos(0, 33);
      case 40:
         return jjStopAtPos(0, 16);
      case 41:
         return jjStopAtPos(0, 17);
      case 42:
         return jjStopAtPos(0, 10);
      case 43:
         return jjStopAtPos(0, 31);
      case 44:
         return jjStopAtPos(0, 28);
      case 45:
         return jjStopAtPos(0, 32);
      case 47:
         jjmatchedKind = 11;
         return jjMoveStringLiteralDfa1_0(0x2000000000L);
      case 58:
         jjmatchedKind = 25;
         return jjMoveStringLiteralDfa1_0(0x4000000L);
      case 59:
         return jjStopAtPos(0, 27);
      case 60:
         jjmatchedKind = 21;
         return jjMoveStringLiteralDfa1_0(0x80000L);
      case 61:
         return jjStopAtPos(0, 22);
      case 62:
         jjmatchedKind = 20;
         return jjMoveStringLiteralDfa1_0(0x40000L);
      case 63:
         return jjStopAtPos(0, 34);
      case 91:
         return jjStopAtPos(0, 14);
      case 93:
         return jjStopAtPos(0, 15);
      case 94:
         return jjStopAtPos(0, 36);
      case 123:
         return jjStopAtPos(0, 12);
      case 124:
         jjmatchedKind = 29;
         return jjMoveStringLiteralDfa1_0(0x40000000L);
      case 125:
         return jjStopAtPos(0, 13);
      case 126:
         return jjStopAtPos(0, 24);
      default :
         return jjMoveNfa_0(0, 0);
   }
}
private int jjMoveStringLiteralDfa1_0(long active0)
{
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      jjStopStringLiteralDfa_0(0, active0);
      return 1;
   }
   switch(curChar)
   {
      case 42:
         if ((active0 & 0x2000000000L) != 0L)
            return jjStopAtPos(1, 37);
         break;
      case 58:
         if ((active0 & 0x4000000L) != 0L)
            return jjStopAtPos(1, 26);
         break;
      case 61:
         if ((active0 & 0x40000L) != 0L)
            return jjStopAtPos(1, 18);
         else if ((active0 & 0x80000L) != 0L)
            return jjStopAtPos(1, 19);
         break;
      case 122:
         if ((active0 & 0x40000000L) != 0L)
            return jjStopAtPos(1, 30);
         break;
      default :
         break;
   }
   return jjStartNfa_0(0, active0);
}
static final long[] jjbitVec0 = {
   0xfffffffffffffffeL, 0xffffffffffffffffL, 0xffffffffffffffffL, 0xffffffffffffffffL
};
static final long[] jjbitVec2 = {
   0x0L, 0x0L, 0xffffffffffffffffL, 0xffffffffffffffffL
};
private int jjMoveNfa_0(int startState, int curPos)
{
   int startsAt = 0;
   jjnewStateCnt = 30;
   int i = 1;
   jjstateSet[0] = startState;
   int kind = 0x7fffffff;
   for (;;)
   {
      if (++jjround == 0x7fffffff)
         ReInitRounds();
      if (curChar < 64)
      {
         long l = 1L << curChar;
         do
         {
            switch(jjstateSet[--i])
            {
               case 14:
                  if ((0xffff7bff00000000L & l) != 0L)
                     jjCheckNAddStates(0, 2);
                  break;
               case 0:
                  if ((0x3ff000000000000L & l) != 0L)
                  {
                     if (kind > 3)
                        kind = 3;
                     jjCheckNAddTwoStates(4, 5);
                  }
                  else if ((0x100003600L & l) != 0L)
                  {
                     if (kind > 9)
                        kind = 9;
                     jjCheckNAdd(29);
                  }
                  else if (curChar == 35)
                     jjstateSet[jjnewStateCnt++] = 22;
                  else if (curChar == 47)
                     jjAddStates(3, 4);
                  else if (curChar == 34)
                     jjCheckNAddStates(5, 7);
                  if ((0x3fe000000000000L & l) != 0L)
                  {
                     if (kind > 2)
                        kind = 2;
                     jjCheckNAdd(3);
                  }
                  break;
               case 1:
                  if ((0x3ff200000000000L & l) == 0L)
                     break;
                  if (kind > 1)
                     kind = 1;
                  jjstateSet[jjnewStateCnt++] = 1;
                  break;
               case 2:
                  if ((0x3fe000000000000L & l) == 0L)
                     break;
                  if (kind > 2)
                     kind = 2;
                  jjCheckNAdd(3);
                  break;
               case 3:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 2)
                     kind = 2;
                  jjCheckNAdd(3);
                  break;
               case 4:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 3)
                     kind = 3;
                  jjCheckNAddTwoStates(4, 5);
                  break;
               case 5:
                  if (curChar == 46)
                     jjCheckNAdd(6);
                  break;
               case 6:
                  if ((0x3ff000000000000L & l) == 0L)
                     break;
                  if (kind > 3)
                     kind = 3;
                  jjCheckNAdd(6);
                  break;
               case 7:
               case 11:
                  if (curChar == 34)
                     jjCheckNAddStates(5, 7);
                  break;
               case 8:
                  if ((0xfffffffb00000000L & l) != 0L)
                     jjCheckNAddStates(5, 7);
                  break;
               case 9:
                  if (curChar == 34 && kind > 4)
                     kind = 4;
                  break;
               case 13:
                  if (curChar == 47)
                     jjAddStates(3, 4);
                  break;
               case 15:
                  if ((0xffff7fff00000000L & l) != 0L)
                     jjCheckNAddStates(0, 2);
                  break;
               case 16:
                  if (curChar == 47 && kind > 6)
                     kind = 6;
                  break;
               case 19:
                  if (curChar == 47)
                     jjCheckNAddStates(0, 2);
                  break;
               case 21:
                  if (curChar == 35)
                     jjstateSet[jjnewStateCnt++] = 22;
                  break;
               case 22:
                  if ((0x3ff000000000000L & l) != 0L)
                     jjAddStates(8, 9);
                  break;
               case 23:
                  if ((0x3ff000000000000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 24;
                  break;
               case 24:
                  if ((0x3ff000000000000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 25;
                  break;
               case 25:
                  if ((0x3ff000000000000L & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 26;
                  break;
               case 26:
               case 28:
                  if ((0x3ff000000000000L & l) != 0L)
                     jjCheckNAdd(27);
                  break;
               case 27:
                  if ((0x3ff000000000000L & l) != 0L && kind > 8)
                     kind = 8;
                  break;
               case 29:
                  if ((0x100003600L & l) == 0L)
                     break;
                  if (kind > 9)
                     kind = 9;
                  jjCheckNAdd(29);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else if (curChar < 128)
      {
         long l = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               case 14:
                  if ((0x7fffffffefffffffL & l) != 0L)
                     jjCheckNAddStates(0, 2);
                  else if (curChar == 92)
                     jjCheckNAddTwoStates(19, 18);
                  break;
               case 0:
               case 1:
                  if ((0x7fffffe87fffffeL & l) == 0L)
                     break;
                  if (kind > 1)
                     kind = 1;
                  jjCheckNAdd(1);
                  break;
               case 8:
                  if ((0x7fffffffefffffffL & l) != 0L)
                     jjCheckNAddStates(5, 7);
                  break;
               case 10:
                  if (curChar == 92)
                     jjAddStates(10, 11);
                  break;
               case 12:
                  if (curChar == 92)
                     jjCheckNAddStates(5, 7);
                  break;
               case 15:
                  if ((0x7fffffffefffffffL & l) != 0L)
                     jjCheckNAddStates(0, 2);
                  break;
               case 17:
                  if (curChar == 92)
                     jjCheckNAddTwoStates(18, 19);
                  break;
               case 18:
                  if (curChar == 92)
                     jjCheckNAddStates(0, 2);
                  break;
               case 20:
                  if (curChar == 92)
                     jjCheckNAddTwoStates(19, 18);
                  break;
               case 22:
                  if ((0x7e0000007eL & l) != 0L)
                     jjAddStates(8, 9);
                  break;
               case 23:
                  if ((0x7e0000007eL & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 24;
                  break;
               case 24:
                  if ((0x7e0000007eL & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 25;
                  break;
               case 25:
                  if ((0x7e0000007eL & l) != 0L)
                     jjstateSet[jjnewStateCnt++] = 26;
                  break;
               case 26:
               case 28:
                  if ((0x7e0000007eL & l) != 0L)
                     jjCheckNAdd(27);
                  break;
               case 27:
                  if ((0x7e0000007eL & l) != 0L && kind > 8)
                     kind = 8;
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      else
      {
         int hiByte = (int)(curChar >> 8);
         int i1 = hiByte >> 6;
         long l1 = 1L << (hiByte & 077);
         int i2 = (curChar & 0xff) >> 6;
         long l2 = 1L << (curChar & 077);
         do
         {
            switch(jjstateSet[--i])
            {
               case 14:
               case 15:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                     jjCheckNAddStates(0, 2);
                  break;
               case 8:
                  if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                     jjAddStates(5, 7);
                  break;
               default : break;
            }
         } while(i != startsAt);
      }
      if (kind != 0x7fffffff)
      {
         jjmatchedKind = kind;
         jjmatchedPos = curPos;
         kind = 0x7fffffff;
      }
      ++curPos;
      if ((i = jjnewStateCnt) == (startsAt = 30 - (jjnewStateCnt = startsAt)))
         return curPos;
      try { curChar = input_stream.readChar(); }
      catch(java.io.IOException e) { return curPos; }
   }
}
private int jjMoveStringLiteralDfa0_1()
{
   switch(curChar)
   {
      case 42:
         return jjMoveStringLiteralDfa1_1(0x8000000000L);
      default :
         return 1;
   }
}
private int jjMoveStringLiteralDfa1_1(long active0)
{
   try { curChar = input_stream.readChar(); }
   catch(java.io.IOException e) {
      return 1;
   }
   switch(curChar)
   {
      case 47:
         if ((active0 & 0x8000000000L) != 0L)
            return jjStopAtPos(1, 39);
         break;
      default :
         return 2;
   }
   return 2;
}
static final int[] jjnextStates = {
   15, 16, 17, 14, 20, 8, 9, 10, 23, 28, 11, 12, 
};
private static final boolean jjCanMove_0(int hiByte, int i1, int i2, long l1, long l2)
{
   switch(hiByte)
   {
      case 0:
         return ((jjbitVec2[i2] & l2) != 0L);
      default : 
         if ((jjbitVec0[i1] & l1) != 0L)
            return true;
         return false;
   }
}

/** Token literal values. */
public static final String[] jjstrLiteralImages = {
"", null, null, null, null, null, null, null, null, null, "\52", "\57", 
"\173", "\175", "\133", "\135", "\50", "\51", "\76\75", "\74\75", "\76", "\74", "\75", 
"\41", "\176", "\72", "\72\72", "\73", "\54", "\174", "\174\172", "\53", "\55", 
"\46", "\77", "\44", "\136", "\57\52", null, "\52\57", null, };

/** Lexer state names. */
public static final String[] lexStateNames = {
   "DEFAULT", 
   "COMMENT", 
};

/** Lex State array. */
public static final int[] jjnewLexState = {
   -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
   -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1, -1, 0, -1, 
};
static final long[] jjtoToken = {
   0xffffffff5fL, 
};
static final long[] jjtoSkip = {
   0x10000000000L, 
};
protected SimpleCharStream input_stream;
private final int[] jjrounds = new int[30];
private final int[] jjstateSet = new int[60];
protected char curChar;
/** Constructor. */
public MapCSSParserTokenManager(SimpleCharStream stream){
   if (SimpleCharStream.staticFlag)
      throw new Error("ERROR: Cannot use a static CharStream class with a non-static lexical analyzer.");
   input_stream = stream;
}

/** Constructor. */
public MapCSSParserTokenManager(SimpleCharStream stream, int lexState){
   this(stream);
   SwitchTo(lexState);
}

/** Reinitialise parser. */
public void ReInit(SimpleCharStream stream)
{
   jjmatchedPos = jjnewStateCnt = 0;
   curLexState = defaultLexState;
   input_stream = stream;
   ReInitRounds();
}
private void ReInitRounds()
{
   int i;
   jjround = 0x80000001;
   for (i = 30; i-- > 0;)
      jjrounds[i] = 0x80000000;
}

/** Reinitialise parser. */
public void ReInit(SimpleCharStream stream, int lexState)
{
   ReInit(stream);
   SwitchTo(lexState);
}

/** Switch to specified lex state. */
public void SwitchTo(int lexState)
{
   if (lexState >= 2 || lexState < 0)
      throw new TokenMgrError("Error: Ignoring invalid lexical state : " + lexState + ". State unchanged.", TokenMgrError.INVALID_LEXICAL_STATE);
   else
      curLexState = lexState;
}

protected Token jjFillToken()
{
   final Token t;
   final String curTokenImage;
   final int beginLine;
   final int endLine;
   final int beginColumn;
   final int endColumn;
   String im = jjstrLiteralImages[jjmatchedKind];
   curTokenImage = (im == null) ? input_stream.GetImage() : im;
   beginLine = input_stream.getBeginLine();
   beginColumn = input_stream.getBeginColumn();
   endLine = input_stream.getEndLine();
   endColumn = input_stream.getEndColumn();
   t = Token.newToken(jjmatchedKind, curTokenImage);

   t.beginLine = beginLine;
   t.endLine = endLine;
   t.beginColumn = beginColumn;
   t.endColumn = endColumn;

   return t;
}

int curLexState = 0;
int defaultLexState = 0;
int jjnewStateCnt;
int jjround;
int jjmatchedPos;
int jjmatchedKind;

/** Get the next Token. */
public Token getNextToken() 
{
  Token matchedToken;
  int curPos = 0;

  EOFLoop :
  for (;;)
  {   
   try   
   {     
      curChar = input_stream.BeginToken();
   }     
   catch(java.io.IOException e)
   {        
      jjmatchedKind = 0;
      matchedToken = jjFillToken();
      return matchedToken;
   }

   switch(curLexState)
   {
     case 0:
       jjmatchedKind = 0x7fffffff;
       jjmatchedPos = 0;
       curPos = jjMoveStringLiteralDfa0_0();
       if (jjmatchedPos == 0 && jjmatchedKind > 38)
       {
          jjmatchedKind = 38;
       }
       break;
     case 1:
       jjmatchedKind = 0x7fffffff;
       jjmatchedPos = 0;
       curPos = jjMoveStringLiteralDfa0_1();
       if (jjmatchedPos == 0 && jjmatchedKind > 40)
       {
          jjmatchedKind = 40;
       }
       break;
   }
     if (jjmatchedKind != 0x7fffffff)
     {
        if (jjmatchedPos + 1 < curPos)
           input_stream.backup(curPos - jjmatchedPos - 1);
        if ((jjtoToken[jjmatchedKind >> 6] & (1L << (jjmatchedKind & 077))) != 0L)
        {
           matchedToken = jjFillToken();
       if (jjnewLexState[jjmatchedKind] != -1)
         curLexState = jjnewLexState[jjmatchedKind];
           return matchedToken;
        }
        else
        {
         if (jjnewLexState[jjmatchedKind] != -1)
           curLexState = jjnewLexState[jjmatchedKind];
           continue EOFLoop;
        }
     }
     int error_line = input_stream.getEndLine();
     int error_column = input_stream.getEndColumn();
     String error_after = null;
     boolean EOFSeen = false;
     try { input_stream.readChar(); input_stream.backup(1); }
     catch (java.io.IOException e1) {
        EOFSeen = true;
        error_after = curPos <= 1 ? "" : input_stream.GetImage();
        if (curChar == '\n' || curChar == '\r') {
           error_line++;
           error_column = 0;
        }
        else
           error_column++;
     }
     if (!EOFSeen) {
        input_stream.backup(1);
        error_after = curPos <= 1 ? "" : input_stream.GetImage();
     }
     throw new TokenMgrError(EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrError.LEXICAL_ERROR);
  }
}

private void jjCheckNAdd(int state)
{
   if (jjrounds[state] != jjround)
   {
      jjstateSet[jjnewStateCnt++] = state;
      jjrounds[state] = jjround;
   }
}
private void jjAddStates(int start, int end)
{
   do {
      jjstateSet[jjnewStateCnt++] = jjnextStates[start];
   } while (start++ != end);
}
private void jjCheckNAddTwoStates(int state1, int state2)
{
   jjCheckNAdd(state1);
   jjCheckNAdd(state2);
}

private void jjCheckNAddStates(int start, int end)
{
   do {
      jjCheckNAdd(jjnextStates[start]);
   } while (start++ != end);
}

}
