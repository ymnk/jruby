// created by jay 1.0 (c) 2002 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "DefaultRubyParser.y"
/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Mirko Stocker <me@misto.ch>
 * Copyright (C) 2006 Thomas Corbat <tcorbat@hsr.ch>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * 
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.parser;

import java.io.IOException;
import java.math.BigInteger;
import org.jruby.ast.AliasNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode; 
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NotNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.ZeroArgNode;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.types.INameNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.ISourcePositionHolder;
import org.jruby.lexer.yacc.LexState;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.lexer.yacc.RubyYaccLexer;
import org.jruby.lexer.yacc.StrTerm;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.lexer.yacc.Token;
import org.jruby.runtime.Visibility;
import org.jruby.util.IdUtil;

public class DefaultRubyParser {
    private ParserSupport support;
    private RubyYaccLexer lexer;
    private IRubyWarnings warnings;

    public DefaultRubyParser() {
        support = new ParserSupport();
        lexer = new RubyYaccLexer();
        lexer.setParserSupport(support);
    }

    public void setWarnings(IRubyWarnings warnings) {
        this.warnings = warnings;

        support.setWarnings(warnings);
        lexer.setWarnings(warnings);
    }

/*
%union {
    Node *node;
    VALUE val;
    ID id;
    int num;
    struct RVarmap *vars;
}
*/
					// line 166 "-"
  // %token constants
  public static final int kCLASS = 257;
  public static final int kMODULE = 258;
  public static final int kDEF = 259;
  public static final int kUNDEF = 260;
  public static final int kBEGIN = 261;
  public static final int kRESCUE = 262;
  public static final int kENSURE = 263;
  public static final int kEND = 264;
  public static final int kIF = 265;
  public static final int kUNLESS = 266;
  public static final int kTHEN = 267;
  public static final int kELSIF = 268;
  public static final int kELSE = 269;
  public static final int kCASE = 270;
  public static final int kWHEN = 271;
  public static final int kWHILE = 272;
  public static final int kUNTIL = 273;
  public static final int kFOR = 274;
  public static final int kBREAK = 275;
  public static final int kNEXT = 276;
  public static final int kREDO = 277;
  public static final int kRETRY = 278;
  public static final int kIN = 279;
  public static final int kDO = 280;
  public static final int kDO_COND = 281;
  public static final int kDO_BLOCK = 282;
  public static final int kRETURN = 283;
  public static final int kYIELD = 284;
  public static final int kSUPER = 285;
  public static final int kSELF = 286;
  public static final int kNIL = 287;
  public static final int kTRUE = 288;
  public static final int kFALSE = 289;
  public static final int kAND = 290;
  public static final int kOR = 291;
  public static final int kNOT = 292;
  public static final int kIF_MOD = 293;
  public static final int kUNLESS_MOD = 294;
  public static final int kWHILE_MOD = 295;
  public static final int kUNTIL_MOD = 296;
  public static final int kRESCUE_MOD = 297;
  public static final int kALIAS = 298;
  public static final int kDEFINED = 299;
  public static final int klBEGIN = 300;
  public static final int klEND = 301;
  public static final int k__LINE__ = 302;
  public static final int k__FILE__ = 303;
  public static final int tIDENTIFIER = 304;
  public static final int tFID = 305;
  public static final int tGVAR = 306;
  public static final int tIVAR = 307;
  public static final int tCONSTANT = 308;
  public static final int tCVAR = 309;
  public static final int tSTRING_CONTENT = 310;
  public static final int tINTEGER = 311;
  public static final int tFLOAT = 312;
  public static final int tNTH_REF = 313;
  public static final int tBACK_REF = 314;
  public static final int tREGEXP_END = 315;
  public static final int tUPLUS = 316;
  public static final int tUMINUS = 317;
  public static final int tUMINUS_NUM = 318;
  public static final int tPOW = 319;
  public static final int tCMP = 320;
  public static final int tEQ = 321;
  public static final int tEQQ = 322;
  public static final int tNEQ = 323;
  public static final int tGEQ = 324;
  public static final int tLEQ = 325;
  public static final int tANDOP = 326;
  public static final int tOROP = 327;
  public static final int tMATCH = 328;
  public static final int tNMATCH = 329;
  public static final int tDOT = 330;
  public static final int tDOT2 = 331;
  public static final int tDOT3 = 332;
  public static final int tAREF = 333;
  public static final int tASET = 334;
  public static final int tLSHFT = 335;
  public static final int tRSHFT = 336;
  public static final int tCOLON2 = 337;
  public static final int tCOLON3 = 338;
  public static final int tOP_ASGN = 339;
  public static final int tASSOC = 340;
  public static final int tLPAREN = 341;
  public static final int tLPAREN2 = 342;
  public static final int tLPAREN_ARG = 343;
  public static final int tLBRACK = 344;
  public static final int tLBRACE = 345;
  public static final int tLBRACE_ARG = 346;
  public static final int tSTAR = 347;
  public static final int tSTAR2 = 348;
  public static final int tAMPER = 349;
  public static final int tAMPER2 = 350;
  public static final int tTILDE = 351;
  public static final int tPERCENT = 352;
  public static final int tDIVIDE = 353;
  public static final int tPLUS = 354;
  public static final int tMINUS = 355;
  public static final int tLT = 356;
  public static final int tGT = 357;
  public static final int tPIPE = 358;
  public static final int tBANG = 359;
  public static final int tCARET = 360;
  public static final int tLCURLY = 361;
  public static final int tBACK_REF2 = 362;
  public static final int tSYMBEG = 363;
  public static final int tSTRING_BEG = 364;
  public static final int tXSTRING_BEG = 365;
  public static final int tREGEXP_BEG = 366;
  public static final int tWORDS_BEG = 367;
  public static final int tQWORDS_BEG = 368;
  public static final int tSTRING_DBEG = 369;
  public static final int tSTRING_DVAR = 370;
  public static final int tSTRING_END = 371;
  public static final int tTAILCOMMENT = 372;
  public static final int tSOLOCOMMENT = 373;
  public static final int tEOF_COMMENT = 374;
  public static final int tLOWEST = 375;
  public static final int tLAST_TOKEN = 376;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal =  1;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 498
    -1,   100,     0,    18,    17,    19,    19,    19,    19,   104,
    20,    20,    20,    20,    20,    20,    20,    20,    20,    20,
   105,    20,    20,    20,    20,    20,    20,    20,    20,    20,
    20,    20,    20,    20,    20,    21,    21,    21,    21,    21,
    21,    29,    25,    25,    25,    25,    25,    48,    48,    48,
   106,    63,    24,    24,    24,    24,    24,    24,    24,    24,
    69,    69,    71,    71,    70,    70,    70,    70,    70,    70,
    65,    65,    74,    74,    66,    66,    66,    66,    66,    66,
    66,    66,    59,    59,    59,    59,    59,    59,    59,    59,
    91,    91,    16,    16,    16,    92,    92,    92,    92,    92,
    85,    85,    54,   108,    54,    93,    93,    93,    93,    93,
    93,    93,    93,    93,    93,    93,    93,    93,    93,    93,
    93,    93,    93,    93,    93,    93,    93,    93,    93,    93,
    93,   107,   107,   107,   107,   107,   107,   107,   107,   107,
   107,   107,   107,   107,   107,   107,   107,   107,   107,   107,
   107,   107,   107,   107,   107,   107,   107,   107,   107,   107,
   107,   107,   107,   107,   107,   107,   107,   107,   107,   107,
   107,   107,    22,    22,    22,    22,    22,    22,    22,    22,
    22,    22,    22,    22,    22,    22,    22,    22,    22,    22,
    22,    22,    22,    22,    22,    22,    22,    22,    22,    22,
    22,    22,    22,    22,    22,    22,    22,    22,    22,    22,
    22,    22,    22,   110,    22,    22,    22,    67,    78,    78,
    78,    78,    78,    78,    36,    36,    36,    36,    37,    37,
    38,    38,    38,    38,    38,    38,    38,    38,    38,    39,
    39,    39,    39,    39,    39,    39,    39,    39,    39,    39,
    39,   112,    41,    40,   113,    40,   114,    40,    44,    43,
    43,    72,    72,    64,    64,    64,    23,    23,    23,    23,
    23,    23,    23,    23,    23,    23,   115,    23,    23,    23,
    23,    23,    23,    23,    23,    23,    23,    23,   116,    23,
    23,    23,    23,    23,    23,   118,   120,    23,   121,   122,
    23,    23,    23,    23,   123,   124,    23,   126,    23,   127,
   128,    23,   129,    23,   130,    23,   131,   132,    23,    23,
    23,    23,    23,    30,   117,   117,   117,   117,   119,   119,
   119,    33,    33,    31,    31,    57,    57,    58,    58,    58,
    58,   133,    62,    47,    47,    47,    26,    26,    26,    26,
    26,    26,   134,    61,   135,    61,    68,    73,    73,    73,
    32,    32,    79,    79,    77,    77,    77,    34,    34,    35,
    35,    13,    13,    13,     2,     3,     3,     4,     5,     6,
    10,    10,    28,    28,    12,    12,    11,    11,    27,    27,
     7,     7,     8,     8,     9,   136,     9,   137,     9,    55,
    55,    55,    55,    87,    86,    86,    86,    86,    15,    14,
    14,    14,    14,    80,    80,    80,    80,    80,    80,    80,
    80,    80,    80,    80,    42,    81,    56,    56,    46,   138,
    46,    46,    51,    51,    52,    52,    52,    52,    52,    52,
    52,    52,    52,    94,    94,    94,    94,    96,    96,    53,
    84,    84,    98,    98,    95,    95,    99,    99,    50,    49,
    49,     1,   139,     1,    83,    83,    83,    75,    75,    76,
    88,    88,    88,    89,    89,    89,    89,    90,    90,    90,
    97,    97,   102,   102,   109,   109,   111,   111,   111,   125,
   125,   103,   103,    60,    82,    45,   101,   101,
    }, yyLen = {
//yyLen 498
     2,     0,     3,     4,     2,     1,     1,     3,     2,     0,
     4,     3,     3,     3,     2,     3,     3,     3,     3,     3,
     0,     5,     4,     3,     3,     3,     6,     5,     5,     5,
     3,     3,     3,     3,     1,     1,     3,     3,     2,     2,
     1,     1,     1,     1,     2,     2,     2,     1,     4,     4,
     0,     5,     2,     3,     4,     5,     4,     5,     2,     2,
     1,     3,     1,     3,     1,     2,     3,     2,     2,     1,
     1,     3,     2,     3,     1,     4,     3,     3,     3,     3,
     2,     1,     1,     4,     3,     3,     3,     3,     2,     1,
     1,     1,     2,     1,     3,     1,     1,     1,     1,     1,
     1,     1,     1,     0,     4,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     3,     5,     3,     6,     5,     5,     5,     5,
     4,     3,     3,     3,     3,     3,     3,     3,     3,     3,
     4,     4,     2,     2,     3,     3,     3,     3,     3,     3,
     3,     3,     3,     3,     3,     3,     3,     2,     2,     3,
     3,     3,     3,     0,     4,     5,     1,     1,     1,     2,
     2,     5,     2,     3,     3,     4,     4,     6,     1,     1,
     1,     2,     5,     2,     5,     4,     7,     3,     1,     4,
     3,     5,     7,     2,     5,     4,     6,     7,     9,     3,
     1,     0,     2,     1,     0,     3,     0,     4,     2,     2,
     1,     1,     3,     3,     4,     2,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     3,     0,     5,     3,     3,
     2,     4,     3,     3,     1,     4,     3,     1,     0,     6,
     2,     1,     2,     6,     6,     0,     0,     7,     0,     0,
     7,     5,     4,     5,     0,     0,     9,     0,     7,     0,
     0,     8,     0,     5,     0,     6,     0,     0,     9,     1,
     1,     1,     1,     1,     1,     1,     1,     2,     1,     1,
     1,     1,     5,     1,     2,     1,     1,     1,     2,     1,
     3,     0,     5,     2,     4,     4,     2,     4,     4,     3,
     2,     1,     0,     5,     0,     5,     5,     1,     4,     2,
     1,     1,     6,     0,     1,     1,     1,     2,     1,     2,
     1,     1,     1,     1,     1,     1,     2,     3,     3,     3,
     3,     3,     0,     3,     1,     2,     3,     3,     0,     3,
     0,     2,     0,     2,     1,     0,     3,     0,     4,     1,
     1,     1,     1,     2,     1,     1,     1,     1,     3,     1,
     1,     2,     2,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     1,     1,     1,     1,     1,     1,     0,     0,
     3,     1,     4,     2,     6,     4,     4,     2,     4,     2,
     2,     1,     0,     1,     1,     1,     1,     1,     3,     3,
     1,     3,     1,     1,     2,     1,     1,     1,     2,     2,
     0,     1,     0,     5,     1,     2,     2,     1,     3,     3,
     1,     1,     1,     1,     1,     1,     1,     1,     1,     1,
     1,     1,     0,     1,     0,     1,     0,     1,     1,     1,
     1,     1,     2,     0,     0,     0,     0,     1,
    }, yyDefRed = {
//yyDefRed 886
     1,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,   295,   298,     0,     0,     0,   321,   322,     0,
     0,     0,   419,   418,   420,   421,     0,     0,     0,    20,
     0,   423,   422,     0,     0,   415,   414,     0,   417,   409,
   410,   426,   427,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   390,   392,   392,     0,     0,
   267,     0,   375,   268,   269,   270,   271,   266,   371,   373,
     0,     0,     0,     0,     0,     0,     0,    35,     0,     0,
   272,     0,    43,     0,     0,     5,     0,    70,     0,    60,
     0,     0,     0,   372,     0,     0,   319,   320,   284,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,   323,
     0,   273,   424,     0,    93,   312,   140,   151,   141,   164,
   137,   157,   147,   146,   162,   145,   144,   139,   165,   149,
   138,   152,   156,   158,   150,   143,   159,   166,   161,     0,
     0,     0,     0,   136,   155,   154,   167,   168,   169,   170,
   171,   135,   142,   133,   134,     0,     0,     0,    97,     0,
   126,   127,   124,   108,   109,   110,   113,   115,   111,   128,
   129,   116,   117,   462,   121,   120,   107,   125,   123,   122,
   118,   119,   114,   112,   105,   106,   130,     0,   461,   314,
    98,    99,   160,   153,   163,   148,   131,   132,    95,    96,
     0,     0,   102,   101,   100,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   490,   489,     0,     0,
     0,   491,     0,     0,     0,     0,     0,     0,   335,   336,
     0,     0,     0,     0,     0,   230,    45,   238,     0,     0,
     0,   467,    46,    44,     0,    59,     0,     0,   350,    58,
    38,     0,     9,   485,     0,     0,     0,   192,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   218,     0,     0,     0,     0,     0,   464,     0,     0,     0,
     0,    68,     0,   208,   207,    39,   406,   405,   407,     0,
   403,   404,     0,     0,     0,     0,     0,     0,     0,   376,
   497,     2,     4,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   341,   343,   354,
   352,   292,     0,     0,     0,     0,     0,     0,     0,    72,
     0,     0,     0,     0,     0,   346,     0,   290,     0,   411,
   412,     0,    90,     0,    92,   431,   429,     0,     0,     0,
     0,     0,     0,   480,   481,   316,     0,   103,     0,     0,
   275,     0,   326,   325,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   492,     0,     0,
     0,     0,     0,     0,   304,     0,   258,     0,     0,   231,
   260,     0,   233,   286,     0,     0,   253,   252,     0,     0,
     0,     0,     0,    11,    13,    12,     0,   288,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   278,     0,     0,
     0,   219,     0,   487,   220,     0,   222,   282,     0,   466,
   465,   283,     0,     0,     0,     0,   394,   397,   395,   408,
   393,   377,   391,   378,   379,   380,   381,   384,     0,   386,
     0,   387,     0,    15,    16,    17,    18,    19,    36,    37,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   475,     0,     0,   476,     0,     0,     0,     0,   349,     0,
     0,   473,   474,     0,     0,     0,    30,     0,     0,    23,
    31,   261,     0,    24,    33,     0,     0,    66,    73,     0,
    25,    50,    53,     0,     0,   307,     0,     0,     0,     0,
    94,     0,     0,     0,     0,     0,   444,   443,   445,     0,
   453,   452,   457,   456,   441,     0,     0,   450,     0,   447,
     0,     0,     0,     0,     0,   366,   365,     0,     0,     0,
     0,   333,     0,   327,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   302,   330,   329,   296,
   328,   299,     0,     0,     0,     0,     0,     0,     0,   237,
   469,     0,   259,     0,     0,     0,     0,   468,   285,     0,
     0,   256,   250,     0,     0,     0,     0,     0,     0,     0,
     0,   224,    10,     0,     0,     0,    22,     0,     0,     0,
     0,     0,   223,     0,   262,     0,     0,     0,     0,     0,
     0,     0,   383,   385,   389,     0,   339,     0,     0,   337,
     0,     0,     0,     0,   229,   347,     0,   228,     0,     0,
   348,     0,     0,   344,    48,   345,    49,   265,     0,     0,
    71,     0,   310,   430,     0,   281,   313,     0,   317,     0,
     0,     0,   433,     0,   439,     0,   440,     0,   437,   454,
   458,   104,     0,     0,   368,   334,     0,     3,   370,     0,
   331,     0,     0,     0,     0,     0,     0,   301,   303,   359,
     0,     0,     0,     0,     0,     0,     0,     0,   235,     0,
     0,     0,     0,     0,   243,   255,   225,     0,     0,   226,
     0,     0,     0,    21,   277,     0,     0,     0,   399,   400,
   401,   396,   402,     0,   338,     0,     0,     0,     0,     0,
    27,     0,    28,    55,     0,    29,     0,    57,     0,     0,
     0,     0,     0,     0,     0,   463,     0,   449,     0,   315,
     0,   459,   451,     0,     0,   448,     0,     0,     0,     0,
   367,     0,     0,   369,     0,   293,     0,   294,     0,     0,
     0,     0,   305,   232,     0,   234,   249,   257,     0,   240,
     0,     0,     0,     0,   289,   221,   398,   340,   342,   355,
   353,     0,    26,   264,     0,     0,   308,     0,   432,   438,
     0,   435,   436,     0,     0,     0,     0,     0,     0,   358,
   360,   356,   361,   297,   300,     0,     0,     0,     0,   239,
     0,   245,     0,   227,    51,   311,     0,     0,     0,     0,
     0,     0,     0,   362,     0,     0,   236,   241,     0,     0,
     0,   244,   318,   434,     0,   332,   306,     0,     0,   246,
     0,   242,     0,   247,     0,   248,
    }, yyDgoto = {
//yyDgoto 140
     1,   187,    60,    61,    62,    63,    64,   292,   289,   460,
    65,    66,   468,    67,    68,    69,   108,   205,   206,    71,
    72,    73,    74,    75,    76,    77,    78,   298,   296,   209,
   258,   710,   841,   711,   703,   707,   664,   665,   236,   621,
   417,   245,    80,   409,   612,   410,   367,    81,    82,   694,
   781,   565,   566,   567,   201,   751,   211,   227,   658,   212,
    85,   357,   338,   542,   530,    86,    87,   238,   396,    88,
    89,   266,   271,   595,    90,   272,   241,   578,   273,   379,
   213,   214,   276,   277,   568,   202,   290,    93,   113,   546,
   518,   114,   204,   513,   569,   570,   571,   375,   572,   573,
     2,   301,   219,   220,   426,   255,   681,   191,   574,   254,
   428,   444,   246,   625,   731,   439,   633,   384,   222,   599,
   722,   223,   723,   607,   845,   385,   684,   543,   773,   371,
   376,   554,   776,   506,   508,   507,   651,   650,   544,   372,
    }, yySindex = {
//yySindex 886
     0,     0, 13938, 14163,  4985, 16963, 17513, 17406, 13938,  3583,
  3583, 13594,     0,     0,  5452, 14387, 14387,     0,     0, 14387,
  -301,  -254,     0,     0,     0,     0,  3583, 17299,    91,     0,
  -265,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0, 16515, 16515,  -147,  -152, 14051,  3583,  4059,
 16515, 17075, 16515, 16627, 17619,     0,     0,     0,   127,   141,
     0,  -157,     0,     0,     0,     0,     0,     0,     0,     0,
  -153,    74,   662,   297,  3036,     0,   -56,     0,  -214,    86,
     0,   -61,     0,   -90,   197,     0,   211,     0,   222,     0,
 16851,     0,   -44,     0,  -203,   662,     0,     0,     0,  -301,
  -254,    91,     0,     0,   217,  3583,   -41, 13938,  -210,     0,
   109,     0,     0,  -203,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,   124,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
 17619,   278,     0,     0,     0,    94,    96,    83,   297,    84,
   290,    38,   338,     0,    64,    84,     0,     0,    74,     9,
   366,     0,  3583,  3583,   158,   296,     0,   156,     0,     0,
     0, 16515, 16515, 16515,  3036,     0,     0,     0,   137,   458,
   466,     0,     0,     0, 13713,     0, 14499, 14387,     0,     0,
     0,   343,     0,     0,   200,   164, 13938,     0,   377,   232,
   240,   247,   229, 14051,   495,     0,   535,   297, 16515,    91,
     0,    49,    69,   489,   210,    69,     0,   461,   286,   435,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  -208,
     0,     0,  -202,   314,  -118,   239,   357,   263,  -239,     0,
     0,     0,     0, 13825,  3583,  3583,  3583,  3583, 14163,  3583,
  3583, 16515, 16515, 16515, 16515, 16515, 16515, 16515, 16515, 16515,
 16515, 16515, 16515, 16515, 16515, 16515, 16515, 16515, 16515, 16515,
 16515, 16515, 16515, 16515, 16515, 16515, 16515,     0,     0,     0,
     0,     0,  2635,  3111,  4059,  3473,  3473, 16627, 15731,     0,
 15731, 14051, 17075,   574, 16627,     0,   293,     0,   200,     0,
     0,   297,     0,     0,     0,     0,     0,    74,  3473,  3652,
  4059, 13938,  3583,     0,     0,     0,  1614,     0, 15843,   372,
     0,   229,     0,     0, 13938,   383,  3953,  4128,  4059, 16515,
 16515, 16515, 13938,   394, 13938, 15955,   404,     0,   476,   476,
     0,  4574,  5050,  4059,     0,   629,     0, 16515, 14611,     0,
     0, 14723,     0,     0,   636, 14275,     0,     0,   -56,    91,
    29,   635,   647,     0,     0,     0, 17406,     0, 16515, 13938,
   571,  3953,  4128, 16515, 16515, 16515,   663,     0,     0,    91,
  2026,     0, 16067,     0,     0, 16515,     0,     0, 16515,     0,
     0,     0,     0,  5530,  7452,  4059,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,    20,     0,
   674,     0,   662,     0,     0,     0,     0,     0,     0,     0,
   232,  1716,  1716,  1716,  1716,  2052,  2052,  4405,  2680,  1716,
  1716,  2140,  2140,   195,   195,   232,   814,   232,   232,   227,
   227,  2052,  2052,  1469,  1469,  3173,  -259,  -259,  -259,   374,
     0,   381,  -254,     0,   385,     0,   393,  -254,     0,     0,
   618,     0,     0,  -254,  -254,  3036,     0, 16515,  2560,     0,
     0,     0,   691,     0,     0,     0,   701,     0,     0,  3036,
     0,     0,     0,    74,  3583,     0,  -254,     0,     0,  -254,
     0,   651,   488,    88, 17725,   693,     0,     0,     0,  1413,
     0,     0,     0,     0,     0, 13938,    74,     0,   712,     0,
   727,   736,   483,   486, 17406,     0,     0,     0,   454, 13938,
   539,     0,   369,     0,   467,   470,   491,   393,   734,  2560,
   372,   568,   577, 16515,   800,    84,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,   754,  3583,   499,     0,
     0, 16515,     0,   137,   805, 16515,   137,     0,     0, 16515,
  3036,     0,     0,    21,   810,   815,   818,  3473,  3473,   820,
 14835,     0,     0,  3583,  3036,   737,     0,   232,   232,  3036,
     0,   823,     0, 16515,     0,     0,     0,     0,     0,   773,
 13938,   687,     0,     0,     0, 16515,     0,  4509, 13938,     0,
 13938, 13938, 16627, 16627,     0,     0,   293,     0, 16627, 16515,
     0,   293,   532,     0,     0,     0,     0,     0, 16515, 16179,
     0,  -259,     0,     0, 13938,     0,     0,   831,     0, 16515,
    91,   616,     0,  -138,     0,   332,     0,  1413,     0,     0,
     0,     0, 17187,    84,     0,     0, 13938,     0,     0,  3583,
     0,   620, 16515, 16515, 16515,   543,   646,     0,     0,     0,
 16291, 13938, 13938, 13938,     0,   476,   629, 14947,     0,   629,
   629,   855, 15059, 15171,     0,     0,     0,  -254,  -254,     0,
   -56,    29,   206,     0,     0,  2026,     0,   786,     0,     0,
     0,     0,     0,  3036,     0,   540,   648,   655,   809,  3036,
     0,  3036,     0,     0,  3036,     0,  3036,     0, 16627,  3036,
 16515,     0, 13938, 13938,   661,     0,  1614,     0,   894,     0,
   693,     0,     0,   727,   896,     0,   727,   637,   436,     0,
     0,     0, 13938,     0,    84,     0, 16515,     0, 16515,    56,
   685,   686,     0,     0, 16515,     0,     0,     0, 16515,     0,
   916,   919, 16515,   926,     0,     0,     0,     0,     0,     0,
     0,  3036,     0,     0,   843,   705,     0, 13938,     0,     0,
  -138,     0,     0,     0, 17784, 17843,  4059,    94, 13938,     0,
     0,     0,     0,     0,     0, 13938,  2978,   629, 15283,     0,
 15395,     0,   629,     0,     0,     0,   709,   727,     0,     0,
     0,     0,   887,     0,   369,   717,     0,     0, 16515,   938,
 16515,     0,     0,     0,     0,     0,     0,   629, 15507,     0,
   629,     0, 16515,     0,   629,     0,
    }, yyRindex = {
//yyRindex 886
     0,     0,    48,     0,     0,     0,     0,     0,   806,     0,
     0,   236,     0,     0,     0,  7575,  7692,     0,     0,  7791,
  4349,  3759,     0,     0,     0,     0,     0,     0, 16403,     0,
     0,     0,     0,  1850,  2921,     0,     0,  1969,     0,     0,
     0,     0,     0,     0,     0,     0,     0,    82,     0,   891,
   861,   142,     0,     0,   376,     0,     0,     0,   547,  -236,
     0,  6752,     0,     0,     0,     0,     0,     0,     0,     0,
   988,   750,   836,  5784,  1568,  6870,  6013,     0,  7055,     0,
     0,  6994,     0,  8948,     0,     0,     0,     0,     0,     0,
   163,  8830,     0,     0, 15619,   965,     0,     0,     0,  7173,
  5769,   653, 12668, 12781,     0,     0,     0,    82,   121,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,  1155,
  1187,  1407,  1589,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,  1651,  2089,  2210,     0,  2525,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 12625,     0,     0,     0,   550,     0,     0,  1723,     0,
     0,  6319,     0,  6103,     0,     0,     0,     0,   728,     0,
   283,     0,     0,     0,     0,     0,   107,     0,     0,     0,
   440,     0,     0,     0, 11425,     0,     0,     0, 12214, 12314,
 12314,     0,     0,     0,     0,     0,     0,   961,     0,     0,
     0,     0,     0,     0, 16739,     0,    70,     0,     0,  7909,
  7272,  7389,  9072,    82,     0,   198,     0,    73,     0,   910,
     0,   915,   915,     0,   892,   892,     0,     0,     0,     0,
   462,     0,   634,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,   796,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,   891,     0,     0,     0,     0,     0,
     0,    82,   199,   209,     0,     0, 12461,     0,     0,     0,
     0,   176,     0, 13131,     0,     0,     0,     0,     0,     0,
   891,   806,     0,     0,     0,     0,   183,     0,    14,   430,
     0,  6536,     0,     0,   824, 13244,     0,     0,   891,     0,
     0,     0,   548,     0,    79,     0,     0,     0,     0,     0,
   521,     0,     0,   891,     0, 12314,     0,     0,     0,     0,
     0,     0,     0,     0,     0,   979,     0,     0,   135,   984,
   984,   223,     0,     0,     0,     0,     0,     0,     0,    70,
     0,     0,     0,     0,     0,     0,     0,     0,   212,   984,
   910,     0,   923,     0,     0,    -6,     0,     0,   898,     0,
     0,     0,   990,     0,     0,   891,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,  1213,     0,     0,     0,     0,     0,     0,     0,
  8094, 10567, 10693, 10783, 10870, 10108, 10232, 10968, 11233, 11053,
 11148, 11328, 11365,  9530,  9647,  8212,  9774,  8311,  8428,  9313,
  9431, 10351, 10449,  9891,  9990,     0, 13362, 13362, 13475,  4710,
     0,  4824,  3873,     0,  5186,  3283,  5300, 15619,     0,  3397,
     0,     0,     0,  5661,  5661, 11462,     0,     0,  1610,     0,
     0,     0,     0,     0,     0,  9205,     0,     0,     0, 11557,
     0,     0,     0,     0,     0,     0,  6202, 12899, 13012,     0,
     0,     0,     0,   984,     0,   143,     0,     0,     0,   238,
     0,     0,     0,     0,     0,   806,     0,     0,   186,     0,
   186,   186,   573,     0,     0,     0,     0,   116,   603,   512,
   763,     0,   763,     0,  2331,  2445,  2807,  4235,     0, 12277,
   763,     0,     0,     0,   632,     0,     0,     0,     0,     0,
     0,     0,   202,   982,  1253,   816,     0,     0,     0,     0,
     0,     0,     0, 12374, 12314,     0,     0,     0,     0,     0,
   184,     0,     0,     0,   987,     0,     0,     0,     0,     0,
     0,     0,     0,     0, 11642,     0,     0,  8614,  8731, 11704,
   575,     0,     0,     0,     0,  1084,  1110,  6261,  2500,     0,
    70,     0,     0,     0,     0,     0,     0,     0,    79,     0,
    79,    70,     0,     0,     0,     0, 12521,     0,     0,     0,
     0, 12589,  9191,     0,     0,     0,     0,     0,     0,     0,
     0, 13475,     0,     0,   806,     0,     0,     0,     0,     0,
   984,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,    79,     0,     0,     0,
     0,     0,     0,     0,     0,  6653,     0,     0,     0,     0,
     0,   452,    79,    79,   853,     0, 12314,     0,     0, 12314,
   987,     0,     0,     0,     0,     0,     0,   130,   130,     0,
     0,   984,     0,     0,     0,   910,  1439,     0,     0,     0,
     0,     0,     0, 11774,     0,     0,     0,     0,     0, 11834,
     0, 11871,     0,     0, 11969,     0, 12063,     0,     0, 12100,
     0,  8033,    70,   806,     0,     0,   183,     0,     0,     0,
     0,     0,     0,   186,   186,     0,   186,     0,     0,   564,
     0,   611,   806,     0,     0,     0,     0,     0,     0,   763,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
   987,   987,     0,     0,     0,     0,     0,     0,     0,     0,
     0, 12160,     0,     0,     0,     0,     0,   806,     0,     0,
     0,     0,     0,   642,     0,     0,   891,   550,   824,     0,
     0,     0,     0,     0,     0,    79, 12314,   987,     0,     0,
     0,     0,   987,     0,     0,     0,     0,   186,     4,   203,
   601,   585,     0,     0,   763,     0,     0,     0,     0,   987,
     0,     0,     0,     0,   720,     0,     0,   987,     0,     0,
   987,     0,     0,     0,   987,     0,
    }, yyGindex = {
//yyGindex 140
     0,     0,     0,     0,   968,     0,     0,     0,   711,  -211,
     0,     0,     0,     0,     0,     0,  1025,   793,  -335,     0,
    23,  1396,   -15,    46,   106,    47,     0,     0,     0,    65,
   101,  -371,     0,   168,     0,     0,    24,  -258,    45,     0,
     0,    -4,  1027,   194,    -3,     0,     0,  -215,     0,  -285,
  -345,   259,   477,  -660,     0,     0,   735,   382,  -319,   975,
   752,   964,     0,  -580,  -193,   954,   -24,  1197,  -375,   -11,
    81,  -207,   -10,     0,     0,    28,  -391,     0,  -321,   208,
   900,  1007,   801,     0,   350,   -12,     0,     3,   582,  -164,
     0,   -87,     1,    15,   355,  -640,     0,     0,     0,     0,
     0,     0,   -46,   991,     0,     0,     0,     0,     0,    98,
     0,   331,     0,     0,     0,     0,     0,  -213,     0,  -388,
     0,     0,     0,     0,     0,    59,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
    };
    protected static final short[] yyTable = YyTables.yyTable();
    protected static final short[] yyCheck = YyTables.yyCheck();

  /** maps symbol value to printable name.
      @see #yyExpecting
    */
  protected static final String[] yyNames = {
    "end-of-file",null,null,null,null,null,null,null,null,null,"'\\n'",
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,"' '",null,null,null,null,null,
    null,null,null,"')'",null,null,"','",null,null,null,null,null,null,
    null,null,null,null,null,null,null,"':'","';'",null,"'='",null,"'?'",
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,
    "'['",null,"']'",null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,"'}'",null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    "kCLASS","kMODULE","kDEF","kUNDEF","kBEGIN","kRESCUE","kENSURE",
    "kEND","kIF","kUNLESS","kTHEN","kELSIF","kELSE","kCASE","kWHEN",
    "kWHILE","kUNTIL","kFOR","kBREAK","kNEXT","kREDO","kRETRY","kIN",
    "kDO","kDO_COND","kDO_BLOCK","kRETURN","kYIELD","kSUPER","kSELF",
    "kNIL","kTRUE","kFALSE","kAND","kOR","kNOT","kIF_MOD","kUNLESS_MOD",
    "kWHILE_MOD","kUNTIL_MOD","kRESCUE_MOD","kALIAS","kDEFINED","klBEGIN",
    "klEND","k__LINE__","k__FILE__","tIDENTIFIER","tFID","tGVAR","tIVAR",
    "tCONSTANT","tCVAR","tSTRING_CONTENT","tINTEGER","tFLOAT","tNTH_REF",
    "tBACK_REF","tREGEXP_END","tUPLUS","tUMINUS","tUMINUS_NUM","tPOW",
    "tCMP","tEQ","tEQQ","tNEQ","tGEQ","tLEQ","tANDOP","tOROP","tMATCH",
    "tNMATCH","tDOT","tDOT2","tDOT3","tAREF","tASET","tLSHFT","tRSHFT",
    "tCOLON2","tCOLON3","tOP_ASGN","tASSOC","tLPAREN","tLPAREN2",
    "tLPAREN_ARG","tLBRACK","tLBRACE","tLBRACE_ARG","tSTAR","tSTAR2",
    "tAMPER","tAMPER2","tTILDE","tPERCENT","tDIVIDE","tPLUS","tMINUS",
    "tLT","tGT","tPIPE","tBANG","tCARET","tLCURLY","tBACK_REF2","tSYMBEG",
    "tSTRING_BEG","tXSTRING_BEG","tREGEXP_BEG","tWORDS_BEG","tQWORDS_BEG",
    "tSTRING_DBEG","tSTRING_DVAR","tSTRING_END","tTAILCOMMENT",
    "tSOLOCOMMENT","tEOF_COMMENT","tLOWEST","tLAST_TOKEN",
    };

  /** thrown for irrecoverable syntax errors and stack overflow.
      Nested for convenience, does not depend on parser class.
    */
  public static class yyException extends java.lang.Exception {
    private static final long serialVersionUID = 1L;
    public yyException (String message) {
      super(message);
    }
  }

  /** must be implemented by a scanner object to supply input to the parser.
      Nested for convenience, does not depend on parser class.
    */
  public interface yyInput {

    /** move on to next token.
        @return <tt>false</tt> if positioned beyond tokens.
        @throws IOException on input error.
      */
    boolean advance () throws java.io.IOException;

    /** classifies current token.
        Should not be called if {@link #advance()} returned <tt>false</tt>.
        @return current <tt>%token</tt> or single character.
      */
    int token ();

    /** associated with current token.
        Should not be called if {@link #advance()} returned <tt>false</tt>.
        @return value for {@link #token()}.
      */
    Object value ();
  }

  /** simplified error message.
      @see #yyerror(java.lang.String, java.lang.String[])
    */
  public void yyerror (String message) {
    yyerror(message, null, null);
  }

  /** (syntax) error message.
      Can be overwritten to control message format.
      @param message text to be displayed.
      @param expected list of acceptable tokens, if available.
    */
  public void yyerror (String message, String[] expected, String found) {
    StringBuffer text = new StringBuffer(message);

    if (expected != null && expected.length > 0) {
      text.append(", expecting");
      for (int n = 0; n < expected.length; ++ n) {
        text.append("\t").append(expected[n]);
      }
      text.append(" but found " + found + " instead\n");
    }

    throw new SyntaxException(getPosition(null), text.toString());
  }

  /** computes list of expected tokens on error by tracing the tables.
      @param state for which to compute the list.
      @return list of token names.
    */
  protected String[] yyExpecting (int state) {
    int token, n, len = 0;
    boolean[] ok = new boolean[yyNames.length];

    if ((n = yySindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < yyNames.length && n+token < yyTable.length; ++ token)
        if (yyCheck[n+token] == token && !ok[token] && yyNames[token] != null) {
          ++ len;
          ok[token] = true;
        }
    if ((n = yyRindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < yyNames.length && n+token < yyTable.length; ++ token)
        if (yyCheck[n+token] == token && !ok[token] && yyNames[token] != null) {
          ++ len;
          ok[token] = true;
        }

    String result[] = new String[len];
    for (n = token = 0; n < len;  ++ token)
      if (ok[token]) result[n++] = yyNames[token];
    return result;
  }

  /** the generated parser, with debugging messages.
      Maintains a dynamic state and value stack.
      @param yyLex scanner.
      @param yydebug debug message writer implementing <tt>yyDebug</tt>, or <tt>null</tt>.
      @return result of the last reduction, if any.
      @throws yyException on irrecoverable parse error.
    */
  public Object yyparse (RubyYaccLexer yyLex, Object ayydebug)
				throws java.io.IOException, yyException {
    return yyparse(yyLex);
  }

  /** initial size and increment of the state/value stack [default 256].
      This is not final so that it can be overwritten outside of invocations
      of {@link #yyparse}.
    */
  protected int yyMax;

  /** executed at the beginning of a reduce action.
      Used as <tt>$$ = yyDefault($1)</tt>, prior to the user-specified action, if any.
      Can be overwritten to provide deep copy, etc.
      @param first value for <tt>$1</tt>, or <tt>null</tt>.
      @return first.
    */
  protected Object yyDefault (Object first) {
    return first;
  }

  /** the generated parser.
      Maintains a dynamic state and value stack.
      @param yyLex scanner.
      @return result of the last reduction, if any.
      @throws yyException on irrecoverable parse error.
    */
  public Object yyparse (RubyYaccLexer yyLex) throws java.io.IOException, yyException {
    if (yyMax <= 0) yyMax = 256;			// initial size
    int yyState = 0, yyStates[] = new int[yyMax];	// state stack
    Object yyVal = null, yyVals[] = new Object[yyMax];	// value stack
    int yyToken = -1;					// current input
    int yyErrorFlag = 0;				// #tokens to shift

    yyLoop: for (int yyTop = 0;; ++ yyTop) {
      if (yyTop >= yyStates.length) {			// dynamically increase
        int[] i = new int[yyStates.length+yyMax];
        System.arraycopy(yyStates, 0, i, 0, yyStates.length);
        yyStates = i;
        Object[] o = new Object[yyVals.length+yyMax];
        System.arraycopy(yyVals, 0, o, 0, yyVals.length);
        yyVals = o;
      }
      yyStates[yyTop] = yyState;
      yyVals[yyTop] = yyVal;

      yyDiscarded: for (;;) {	// discarding a token does not change stack
        int yyN;
        if ((yyN = yyDefRed[yyState]) == 0) {	// else [default] reduce (yyN)
          if (yyToken < 0) {
            yyToken = yyLex.advance() ? yyLex.token() : 0;
          }
          if ((yyN = yySindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken) {
            yyState = yyTable[yyN];		// shift to yyN
            yyVal = yyLex.value();
            yyToken = -1;
            if (yyErrorFlag > 0) -- yyErrorFlag;
            continue yyLoop;
          }
          if ((yyN = yyRindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken)
            yyN = yyTable[yyN];			// reduce (yyN)
          else
            switch (yyErrorFlag) {
  
            case 0:
              yyerror("syntax error", yyExpecting(yyState), yyNames[yyToken]);
  
            case 1: case 2:
              yyErrorFlag = 3;
              do {
                if ((yyN = yySindex[yyStates[yyTop]]) != 0
                    && (yyN += yyErrorCode) >= 0 && yyN < yyTable.length
                    && yyCheck[yyN] == yyErrorCode) {
                  yyState = yyTable[yyN];
                  yyVal = yyLex.value();
                  continue yyLoop;
                }
              } while (-- yyTop >= 0);
              throw new yyException("irrecoverable syntax error");
  
            case 3:
              if (yyToken == 0) {
                throw new yyException("irrecoverable syntax error at end-of-file");
              }
              yyToken = -1;
              continue yyDiscarded;		// leave stack alone
            }
        }
        int yyV = yyTop + 1-yyLen[yyN];
        yyVal = yyDefault(yyV > yyTop ? null : yyVals[yyV]);
        switch (yyN) {
case 1:
					// line 332 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();

              }
  break;
case 2:
					// line 336 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) != null) {
                      /* last expression should not be void */
                      if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                          support.checkUselessStatement(((BlockNode)yyVals[-1+yyTop]).getLast());
                      } else {
                          support.checkUselessStatement(((Node)yyVals[-1+yyTop]));
                      }
                  }
                  Node node = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{yyVals[0+yyTop]});
                  support.getResult().setAST(support.appendToBlock(support.getResult().getAST(), node));
                  support.updateTopLocalVariables();
              }
  break;
case 3:
					// line 355 "DefaultRubyParser.y"
  {
                 Node node = ((Node)yyVals[-3+yyTop]);

		 if (((RescueBodyNode)yyVals[-2+yyTop]) != null) {
		   node = new RescueNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]), true), ((Node)yyVals[-3+yyTop]), ((RescueBodyNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
		 } else if (((Node)yyVals[-1+yyTop]) != null) {
		       warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), "else without rescue is useless");
                       node = support.appendToBlock(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
		 }
		 if (((Node)yyVals[0+yyTop]) != null) {
		    node = new EnsureNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), node, ((Node)yyVals[0+yyTop]));
		 }

		 yyVal = node;
             }
  break;
case 4:
					// line 371 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                     support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
		  }
                  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 6:
					// line 379 "DefaultRubyParser.y"
  {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[0+yyTop]), true));
                }
  break;
case 7:
					// line 382 "DefaultRubyParser.y"
  {
		    			Node node = support.introduceComment(((Node)yyVals[0+yyTop]), new Object[]{yyVals[-1+yyTop]});
		    			yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(node, getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]), true)));
                }
  break;
case 8:
					// line 386 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 9:
					// line 390 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 10:
					// line 392 "DefaultRubyParser.y"
  {
                   AliasNode node = new AliasNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-3+yyTop]), ((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 11:
					// line 396 "DefaultRubyParser.y"
  {
              		VAliasNode node = new VAliasNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 12:
					// line 400 "DefaultRubyParser.y"
  {
                    VAliasNode node = new VAliasNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), (String) ((Token)yyVals[-1+yyTop]).getValue(), "$" + ((BackRefNode)((Token)yyVals[0+yyTop]).getValue()).getType()); /* XXX*/
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 13:
					// line 404 "DefaultRubyParser.y"
  {
                    yyerror("can't make alias for the number variables");
                    yyVal = null; /*XXX 0*/
                }
  break;
case 14:
					// line 409 "DefaultRubyParser.y"
  {
                    yyVal = support.introduceComment(((Node)yyVals[0+yyTop]), new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 15:
					// line 412 "DefaultRubyParser.y"
  {
                    IfNode node = new IfNode(support.union(((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]).getPosition()), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 16:
					// line 416 "DefaultRubyParser.y"
  {
                    IfNode node = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 17:
					// line 420 "DefaultRubyParser.y"
  {
                    WhileNode node;
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        node = new WhileNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        node = new WhileNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
                    
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 18:
					// line 430 "DefaultRubyParser.y"
  {
                    UntilNode node;
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                    		node = new UntilNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode());
                    } else {
                        node = new UntilNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]));
                    }
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 19:
					// line 440 "DefaultRubyParser.y"
  {
		  			RescueNode node = new RescueNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), null,((Node)yyVals[0+yyTop]), null), null);
		  			yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 20:
					// line 445 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("BEGIN in method");
                    }
                    support.getLocalNames().push(new LocalNamesElement());
                }
  break;
case 21:
					// line 450 "DefaultRubyParser.y"
  {
                
                		Node node = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), yyVals[0+yyTop]});
                    support.getResult().addBeginNode(new ScopeNode(getPosition(((Token)yyVals[-4+yyTop]), true), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), node));
                    support.getLocalNames().pop();
                    yyVal = null; /*XXX 0;*/
                }
  break;
case 22:
					// line 457 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("END in method; use at_exit");
                    }
                    Node node = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-3+yyTop]),((Token)yyVals[-2+yyTop]),yyVals[0+yyTop]});
                    support.getResult().addEndNode(new IterNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), null, new PostExeNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))), node));
                    yyVal = null;
                }
  break;
case 23:
					// line 465 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    Node node = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{yyVals[-1+yyTop]});
                }
  break;
case 24:
					// line 470 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
		    if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		        ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		    } else {
		        ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((Node)yyVals[0+yyTop])));
		    }
		    yyVal = support.introduceComment(((MultipleAsgnNode)yyVals[-2+yyTop]), new Object[]{yyVals[-1+yyTop]});
                }
  break;
case 25:
					// line 480 "DefaultRubyParser.y"
  {
 		    support.checkExpression(((Node)yyVals[0+yyTop]));
 		    
 		    Object[] tokens = new Object[]{((Token)yyVals[-1+yyTop])};
		    if (((Node)yyVals[-2+yyTop]) != null) {
		        String name = ((INameNode)yyVals[-2+yyTop]).getName();
			String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();
		        if (asgnOp.equals("||")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
	                    OpAsgnOrNode node = new OpAsgnOrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.gettable(name, ((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[-2+yyTop]));
	                    yyVal = support.introduceComment(node, tokens);
			    /* XXX
			    if (is_asgn_or_id(vid)) {
				$$->nd_aid = vid;
			    }
			    */
			} else if (asgnOp.equals("&&")) {
	                    		((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                            OpAsgnAndNode node = new OpAsgnAndNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.gettable(name, ((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[-2+yyTop]));
                            yyVal = support.introduceComment(node, tokens);
			} else {

                            if (((Node)yyVals[-2+yyTop]) != null) {
                                ((AssignableNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable(name, ((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition()), asgnOp, ((Node)yyVals[0+yyTop])));
                            		yyVal = support.introduceComment(((Node)yyVals[-2+yyTop]), tokens);
                            }
                            else{
                                yyVal = ((Node)yyVals[-2+yyTop]);
                            }

			}
		    } else {
 		        yyVal = null;
		    }
		}
  break;
case 26:
					// line 515 "DefaultRubyParser.y"
  {
                    /* Much smaller than ruby block */
                    OpElementAsgnNode node = new OpElementAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{yyVals[-4+yyTop],yyVals[-2+yyTop],((Token)yyVals[-1+yyTop])});

                }
  break;
case 27:
					// line 521 "DefaultRubyParser.y"
  {
                    OpAsgnNode node = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-3+yyTop]),((Token)yyVals[-2+yyTop]),((Token)yyVals[-1+yyTop])});
                }
  break;
case 28:
					// line 525 "DefaultRubyParser.y"
  {
                    OpAsgnNode node = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-3+yyTop]),((Token)yyVals[-2+yyTop]),((Token)yyVals[-1+yyTop])});
                }
  break;
case 29:
					// line 529 "DefaultRubyParser.y"
  {
  					OpAsgnNode node = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
  					yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-3+yyTop]),((Token)yyVals[-2+yyTop]),((Token)yyVals[-1+yyTop])});
                }
  break;
case 30:
					// line 533 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 31:
					// line 537 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), new SValueNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
                }
  break;
case 32:
					// line 540 "DefaultRubyParser.y"
  {
                    if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		        			((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop])));
		    			} else {
		       			 ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((Node)yyVals[0+yyTop])));
		    }
		    yyVal = support.introduceComment(((MultipleAsgnNode)yyVals[-2+yyTop]), new Object[]{yyVals[-1+yyTop]});
		}
  break;
case 33:
					// line 548 "DefaultRubyParser.y"
  {
                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
		    yyVal = support.introduceComment(((MultipleAsgnNode)yyVals[-2+yyTop]), new Object[]{yyVals[-1+yyTop]});
		}
  break;
case 36:
					// line 555 "DefaultRubyParser.y"
  {
                    Node node = support.newAndNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 37:
					// line 559 "DefaultRubyParser.y"
  {
                    Node node = support.newOrNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 38:
					// line 563 "DefaultRubyParser.y"
  {
                    NotNode node = new NotNode(((Token)yyVals[-1+yyTop]).getPosition(), support.getConditionNode(((Node)yyVals[0+yyTop])));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 39:
					// line 567 "DefaultRubyParser.y"
  {
                    NotNode node = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 41:
					// line 573 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
		    yyVal = ((Node)yyVals[0+yyTop]); /*Do we really need this set? $1 is $$?*/
		}
  break;
case 44:
					// line 580 "DefaultRubyParser.y"
  {
                    ReturnNode node = new ReturnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 45:
					// line 584 "DefaultRubyParser.y"
  {
                    BreakNode node = new BreakNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 46:
					// line 588 "DefaultRubyParser.y"
  {
                    NextNode node = new NextNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.ret_args(((Node)yyVals[0+yyTop]), getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 48:
					// line 594 "DefaultRubyParser.y"
  {
                    Node node = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop])});
                }
  break;
case 49:
					// line 598 "DefaultRubyParser.y"
  {
                    Node node = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop])});
                }
  break;
case 50:
					// line 603 "DefaultRubyParser.y"
  {
                      support.getBlockNames().push(new BlockNamesElement());
		  }
  break;
case 51:
					// line 605 "DefaultRubyParser.y"
  {
                      IterNode node = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                      yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-4+yyTop]), yyVals[0+yyTop]});
                      support.getBlockNames().pop();
		  }
  break;
case 52:
					// line 611 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall((String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
                }
  break;
case 53:
					// line 614 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall((String) ((Token)yyVals[-2+yyTop]).getValue(), ((Node)yyVals[-1+yyTop]), ((Token)yyVals[-2+yyTop])); 
	            if (((IterNode)yyVals[0+yyTop]) != null) {
                        if (yyVal instanceof BlockPassNode) {
                            throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), "Both block arg and actual block given.");
                        }
                        ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVal));
                        yyVal = ((Node)yyVals[-1+yyTop]);
		   }
                }
  break;
case 54:
					// line 624 "DefaultRubyParser.y"
  {
                    Node node = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop])});
                }
  break;
case 55:
					// line 628 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
		    if (((IterNode)yyVals[0+yyTop]) != null) {
		        if (yyVal instanceof BlockPassNode) {
                            throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), "Both block arg and actual block given.");
                        }
                        ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVal));
			yyVal = ((IterNode)yyVals[0+yyTop]);
		    }
		 }
  break;
case 56:
					// line 638 "DefaultRubyParser.y"
  {
                    Node node = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop])});
                }
  break;
case 57:
					// line 642 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
				    if (((IterNode)yyVals[0+yyTop]) != null) {
				        if (yyVal instanceof BlockPassNode) {
	                        throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), "Both block arg and actual block given.");
	                    }
	                    ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVal));
					yyVal = ((IterNode)yyVals[0+yyTop]);
			    }
	        }
  break;
case 58:
					// line 652 "DefaultRubyParser.y"
  {
				    Node node = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
				    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
				}
  break;
case 59:
					// line 656 "DefaultRubyParser.y"
  {
                    Node node = support.new_yield(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
		}
  break;
case 61:
					// line 662 "DefaultRubyParser.y"
  {
                    MultipleAsgnNode mlhs = new MultipleAsgnNode(((MultipleAsgnNode)yyVals[-1+yyTop]).getPosition(), ((MultipleAsgnNode)yyVals[-1+yyTop]).getHeadNode(), support.introduceComment(((MultipleAsgnNode)yyVals[-1+yyTop]).getArgsNode(), new Object[]{((Token)yyVals[-2+yyTop]), yyVals[0+yyTop]}));
                    yyVal = mlhs;
				}
  break;
case 63:
					// line 668 "DefaultRubyParser.y"
  {
	            		yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((MultipleAsgnNode)yyVals[-1+yyTop])), null);
                }
  break;
case 64:
					// line 672 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), ((ListNode)yyVals[0+yyTop]), null);
                }
  break;
case 65:
					// line 675 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(support.union(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null);
                    ((Node)yyVals[-1+yyTop]).setPosition(support.union(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])));
                
                }
  break;
case 66:
					// line 680 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((ListNode)yyVals[-2+yyTop]), support.introduceComment(((Node)yyVals[0+yyTop]), new Object[]{((Token)yyVals[-1+yyTop])}));
                }
  break;
case 67:
					// line 683 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((ListNode)yyVals[-1+yyTop]), support.introduceComment(new StarNode(getPosition(null)), new Object[]{((Token)yyVals[0+yyTop])}));
                }
  break;
case 68:
					// line 686 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), null, support.introduceComment(((Node)yyVals[0+yyTop]), new Object[]{((Token)yyVals[-1+yyTop])}));
                }
  break;
case 69:
					// line 689 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, support.introduceComment(new StarNode(getPosition(null)), new Object[]{((Token)yyVals[0+yyTop])}));
                }
  break;
case 71:
					// line 694 "DefaultRubyParser.y"
  {
                    yyVal = support.introduceComment(((MultipleAsgnNode)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop]), yyVals[0+yyTop]});
                }
  break;
case 72:
					// line 698 "DefaultRubyParser.y"
  {
                    
                    yyVal = new ArrayNode(((Node)yyVals[-1+yyTop]).getPosition()).add(support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{yyVals[0+yyTop]}));
                }
  break;
case 73:
					// line 702 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{yyVals[0+yyTop]}));
                }
  break;
case 74:
					// line 706 "DefaultRubyParser.y"
  {
                    Node node = support.assignable(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), yyVals[0+yyTop], null);
                    yyVal = support.introduceComment(node, new Object[]{yyVals[0+yyTop]});
                
                }
  break;
case 75:
					// line 711 "DefaultRubyParser.y"
  {
                    Node node = support.getElementAssignmentNode(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{yyVals[-2+yyTop], yyVals[0+yyTop]});
                }
  break;
case 76:
					// line 715 "DefaultRubyParser.y"
  {
                    Node node = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 77:
					// line 719 "DefaultRubyParser.y"
  {
                    Node node = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 78:
					// line 723 "DefaultRubyParser.y"
  {
                    Node node = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 79:
					// line 727 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
			
                    Node node = new ConstDeclNode(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue(), null);
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
		}
  break;
case 80:
					// line 735 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }

		    /* ERROR:  VEry likely a big error. */
                    Node node = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]),((Token)yyVals[0+yyTop])});
		    /* ruby $$ = NEW_CDECL(0, 0, NEW_COLON3($2)); */
		    }
  break;
case 81:
					// line 746 "DefaultRubyParser.y"
  {
	            support.backrefAssignError(((Node)yyVals[0+yyTop]));
                    yyVal = null;
                }
  break;
case 82:
					// line 751 "DefaultRubyParser.y"
  {
					Node node = support.assignable(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]), true), yyVals[0+yyTop], null);
                    yyVal = support.introduceComment(node, new Object[]{yyVals[0+yyTop]});
                }
  break;
case 83:
					// line 755 "DefaultRubyParser.y"
  {
                    Node node = support.getElementAssignmentNode(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{yyVals[-2+yyTop],yyVals[0+yyTop]});
                }
  break;
case 84:
					// line 759 "DefaultRubyParser.y"
  {
                    Node node = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 85:
					// line 763 "DefaultRubyParser.y"
  {
                    Node node = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
 	        }
  break;
case 86:
					// line 767 "DefaultRubyParser.y"
  {
                    Node node = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 87:
					// line 771 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
                    Node node = new ConstDeclNode(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue(), null);
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
	        }
  break;
case 88:
					// line 778 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }

		    /* ERROR:  VEry likely a big error. */
                    Colon3Node node = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]),((Token)yyVals[0+yyTop])});
		    /* ruby $$ = NEW_CDECL(0, 0, NEW_COLON3($2)); */
	        }
  break;
case 89:
					// line 788 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
                    yyVal = null;
		}
  break;
case 90:
					// line 793 "DefaultRubyParser.y"
  {
                    yyerror("class/module name must be CONSTANT");
                }
  break;
case 92:
					// line 798 "DefaultRubyParser.y"
  {
                    Colon3Node node = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());;
                    Object[] tokens = new Object[]{((Token)yyVals[-1+yyTop]),((Token)yyVals[0+yyTop])};
                    yyVal = support.introduceComment(node, tokens); 
		}
  break;
case 93:
					// line 803 "DefaultRubyParser.y"
  {
                    /* $1 was $$ in ruby?*/
                    Colon2Node node = new Colon2Node(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), null, (String) ((Token)yyVals[0+yyTop]).getValue());
                    Object[] tokens = new Object[]{((Token)yyVals[0+yyTop])};
                    yyVal = support.introduceComment(node, tokens);
                    
 	        }
  break;
case 94:
					// line 810 "DefaultRubyParser.y"
  {
                    Object[] tokens = new Object[]{((Token)yyVals[-1+yyTop])};
                    Colon2Node node = new Colon2Node(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), support.introduceComment(((Node)yyVals[-2+yyTop]), tokens), (String) ((Token)yyVals[0+yyTop]).getValue());
                    
                    tokens = new Object[]{((Token)yyVals[0+yyTop])};
                    yyVal = support.introduceComment(node, tokens);
		}
  break;
case 98:
					// line 821 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 99:
					// line 825 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = yyVals[0+yyTop];
                }
  break;
case 102:
					// line 833 "DefaultRubyParser.y"
  {
                    UndefNode node = new UndefNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 103:
					// line 837 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
	        }
  break;
case 104:
					// line 839 "DefaultRubyParser.y"
  {
	        			Node node = support.appendToBlock(((Node)yyVals[-3+yyTop]), new UndefNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue()));
                    yyVal = support.introduceComment(node, new Object[]{yyVals[-2+yyTop],((Token)yyVals[0+yyTop])});
                }
  break;
case 105:
					// line 844 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("|"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 106:
					// line 845 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("^"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 107:
					// line 846 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("&"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 108:
					// line 847 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<=>"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 109:
					// line 848 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("=="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 110:
					// line 849 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("==="); yyVal = ((Token)yyVals[0+yyTop]);}
  break;
case 111:
					// line 850 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("=~"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 112:
					// line 851 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue(">"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 113:
					// line 852 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue(">="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 114:
					// line 853 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 115:
					// line 854 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 116:
					// line 855 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("<<"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 117:
					// line 856 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue(">>"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 118:
					// line 857 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("+"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 119:
					// line 858 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("-"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 120:
					// line 859 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("*"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 121:
					// line 860 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("*"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 122:
					// line 861 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("/"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 123:
					// line 862 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("%"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 124:
					// line 863 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("**"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 125:
					// line 864 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("~"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 126:
					// line 865 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("+@"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 127:
					// line 866 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("-@"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 128:
					// line 867 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("[]"); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 129:
					// line 868 "DefaultRubyParser.y"
  { ((Token)yyVals[0+yyTop]).setValue("[]="); yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 130:
					// line 869 "DefaultRubyParser.y"
  {  yyVal = ((Token)yyVals[0+yyTop]); }
  break;
case 172:
					// line 880 "DefaultRubyParser.y"
  {
					Node node = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{yyVals[-1+yyTop]});
		    ((Node)yyVal).setPosition(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 173:
					// line 885 "DefaultRubyParser.y"
  {
	      			RescueNode rescNode = new RescueNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), null ,((Node)yyVals[0+yyTop]), null), null);
	      			Node node = support.node_assign(((Node)yyVals[-4+yyTop]), support.introduceComment(rescNode, new Object[]{((Token)yyVals[-1+yyTop])}));
                    yyVal = support.introduceComment(node, new Object[]{yyVals[-3+yyTop]});
		}
  break;
case 174:
					// line 890 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[0+yyTop]));
		    Object[] tokens = new Object[]{((Node)yyVals[-2+yyTop]),((Token)yyVals[-1+yyTop])};
		    if (((Node)yyVals[-2+yyTop]) != null) {
		        String name = ((INameNode)yyVals[-2+yyTop]).getName();
			String asgnOp = (String) ((Token)yyVals[-1+yyTop]).getValue();

		        if (asgnOp.equals("||")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
	                    OpAsgnOrNode node = new OpAsgnOrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.gettable(name, ((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[-2+yyTop]));
	                    yyVal = support.introduceComment(node, tokens);
			    /* FIXME
			    if (is_asgn_or_id(vid)) {
				$$->nd_aid = vid;
			    }
			    */
			} else if (asgnOp.equals("&&")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
	                    OpAsgnAndNode node = new OpAsgnAndNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.gettable(name, ((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition()), ((Node)yyVals[-2+yyTop]));
                            yyVal = support.introduceComment(node, tokens);
			} else {
			    
                            if (((Node)yyVals[-2+yyTop]) != null) {
			      				((AssignableNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable(name, ((ISourcePositionHolder)yyVals[-2+yyTop]).getPosition()), asgnOp, ((Node)yyVals[0+yyTop])));
                            		yyVal = support.introduceComment(((Node)yyVals[-2+yyTop]), tokens);
                            }else{
                            		yyVal = ((Node)yyVals[-2+yyTop]);
                            }
			}
		    } else {
 		        yyVal = null; /* XXX 0; */
		    }
                }
  break;
case 175:
					// line 923 "DefaultRubyParser.y"
  {
                    OpElementAsgnNode node = new OpElementAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Node)yyVals[-5+yyTop]), (String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{yyVals[-4+yyTop], yyVals[-2+yyTop], yyVals[-1+yyTop]});
                }
  break;
case 176:
					// line 927 "DefaultRubyParser.y"
  {
              		OpAsgnNode node = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-3+yyTop]), ((Token)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop])});
                }
  break;
case 177:
					// line 931 "DefaultRubyParser.y"
  {
                    OpAsgnNode node = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-3+yyTop]), ((Token)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop])});
                }
  break;
case 178:
					// line 935 "DefaultRubyParser.y"
  {
                    OpAsgnNode node = new OpAsgnNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), (String) ((Token)yyVals[-2+yyTop]).getValue(), (String) ((Token)yyVals[-1+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-3+yyTop]), ((Token)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop])});
                }
  break;
case 179:
					// line 939 "DefaultRubyParser.y"
  {
				    yyerror("constant re-assignment");
				    yyVal = null;
		        }
  break;
case 180:
					// line 943 "DefaultRubyParser.y"
  {
		    yyerror("constant re-assignment");
		    yyVal = null;
	        }
  break;
case 181:
					// line 947 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 182:
					// line 951 "DefaultRubyParser.y"
  {
				    support.checkExpression(((Node)yyVals[-2+yyTop]));
				    support.checkExpression(((Node)yyVals[0+yyTop]));
                    DotNode node = new DotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false);
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 183:
					// line 957 "DefaultRubyParser.y"
  {
		    			support.checkExpression(((Node)yyVals[-2+yyTop]));
		    			support.checkExpression(((Node)yyVals[0+yyTop]));
                 	DotNode node = new DotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true);
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 184:
					// line 963 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 185:
					// line 967 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 186:
					// line 971 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 187:
					// line 975 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 188:
					// line 979 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 189:
					// line 983 "DefaultRubyParser.y"
  {
		      		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]));
		      		yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                    /* Covert '- number ** number' to '- (number ** number)' 
                    boolean needNegate = false;
                    if (($1 instanceof FixnumNode && $<FixnumNode>1.getValue() < 0) ||
                        ($1 instanceof BignumNode && $<BignumNode>1.getValue().compareTo(BigInteger.ZERO) < 0) ||
                        ($1 instanceof FloatNode && $<FloatNode>1.getValue() < 0.0)) {

                        $<>1 = support.getOperatorCallNode($1, "-@");
                        needNegate = true;
                    }

                    $$ = support.getOperatorCallNode($1, "**", $3);

                    if (needNegate) {
                        $$ = support.getOperatorCallNode($<Node>$, "-@");
                    }
		    */
                }
  break;
case 190:
					// line 1003 "DefaultRubyParser.y"
  {
                    Object number = ((Token)yyVals[-2+yyTop]).getValue();
					Node node = support.getOperatorCallNode(support.getOperatorCallNode((number instanceof Long ? (Node) new FixnumNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Long) number).longValue()) : (Node)new BignumNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((BigInteger) number))), "**", ((Node)yyVals[0+yyTop])), "-@");
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-3+yyTop]), ((Token)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop])});
                }
  break;
case 191:
					// line 1008 "DefaultRubyParser.y"
  {
  /* ENEBO: Seems like this should be $2*/
  /* TCORBAT: Done. Should work so far.*/
  /*$$ = support.getOperatorCallNode(support.getOperatorCallNode(new FloatNode(getPosition($<ISourcePositionHolder>1), ((Double) $1.getValue()).doubleValue()), "**", $4), "-@");*/
                                
                Double number = (Double)((Token)yyVals[-2+yyTop]).getValue();
                Node node = support.getOperatorCallNode(support.getOperatorCallNode(new FloatNode(getPosition(((Token)yyVals[-3+yyTop])), number.doubleValue()), "**", ((Node)yyVals[0+yyTop])), "-@");
                yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-3+yyTop]), ((Token)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop])});
                
                }
  break;
case 192:
					// line 1018 "DefaultRubyParser.y"
  {
              		Node node;
	 	            if (((Node)yyVals[0+yyTop]) != null && ((Node)yyVals[0+yyTop]) instanceof ILiteralNode) {
			        		node = ((Node)yyVals[0+yyTop]);
			    		 }else {
	                    node = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
			    		}
			    		yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 193:
					// line 1027 "DefaultRubyParser.y"
  {
                    Node node = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
				}
  break;
case 194:
					// line 1031 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 195:
					// line 1035 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 196:
					// line 1039 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 197:
					// line 1043 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 198:
					// line 1047 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 199:
					// line 1051 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 200:
					// line 1055 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 201:
					// line 1059 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 202:
					// line 1063 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 203:
					// line 1067 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 204:
					// line 1071 "DefaultRubyParser.y"
  {
                    Node node = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop])));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 205:
					// line 1075 "DefaultRubyParser.y"
  {
                    Node node = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 206:
					// line 1079 "DefaultRubyParser.y"
  {
              		Node node = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 207:
					// line 1083 "DefaultRubyParser.y"
  {
                    Node node = new NotNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])));
                		yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 208:
					// line 1087 "DefaultRubyParser.y"
  {
                    Node node = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 209:
					// line 1091 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 210:
					// line 1095 "DefaultRubyParser.y"
  {
              		Node node = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 211:
					// line 1099 "DefaultRubyParser.y"
  {
                    Node node = support.newAndNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 212:
					// line 1103 "DefaultRubyParser.y"
  {
              		Node node = support.newOrNode(((Token)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 213:
					// line 1107 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 214:
					// line 1109 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    DefinedNode node = new DefinedNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-3+yyTop]),yyVals[-2+yyTop]});
                }
  break;
case 215:
					// line 1114 "DefaultRubyParser.y"
  {
              		IfNode node = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{yyVals[-3+yyTop], yyVals[-1+yyTop]});
                }
  break;
case 216:
					// line 1118 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 217:
					// line 1122 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[0+yyTop]));
	            yyVal = ((Node)yyVals[0+yyTop]);   
		}
  break;
case 219:
					// line 1128 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 220:
					// line 1132 "DefaultRubyParser.y"
  {
                    yyVal = support.commentLastElement(((ListNode)yyVals[-1+yyTop]), new Object[]{yyVals[0+yyTop]});
              }
  break;
case 221:
					// line 1135 "DefaultRubyParser.y"
  {
              		support.commentLastElement(((ListNode)yyVals[-4+yyTop]), new Object[]{yyVals[-3+yyTop]});
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
                    Node node = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop])});
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), node);
                }
  break;
case 222:
					// line 1141 "DefaultRubyParser.y"
  {
              		Node node = support.introduceComment(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])), new Object[]{yyVals[0+yyTop]});
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(node);
                }
  break;
case 223:
					// line 1145 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
                    Node node = support.introduceComment(new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[-1+yyTop])), new Object[]{((Token)yyVals[-2+yyTop])});
		    			yyVal = new NewlineNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), node);
                }
  break;
case 224:
					// line 1151 "DefaultRubyParser.y"
  { 
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 225:
					// line 1154 "DefaultRubyParser.y"
  {
                    yyVal = support.introduceComment(((Node)yyVals[-2+yyTop]), new Object[]{((Token)yyVals[-3+yyTop]), yyVals[0+yyTop]});
                }
  break;
case 226:
					// line 1157 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), "parenthesize argument(s) for future version");
                    
                    Node node = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))).add(((Node)yyVals[-2+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-3+yyTop]),yyVals[0+yyTop]});
                }
  break;
case 227:
					// line 1163 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), "parenthesize argument(s) for future version");
                    support.commentLastElement(((ListNode)yyVals[-4+yyTop]), new Object[]{yyVals[-3+yyTop]});
                    ((ListNode)yyVals[-4+yyTop]).add(((Node)yyVals[-2+yyTop]));
                    yyVal = support.introduceComment(((ListNode)yyVals[-4+yyTop]), new Object[]{((Token)yyVals[-5+yyTop]),yyVals[0+yyTop]});
                }
  break;
case 230:
					// line 1173 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 231:
					// line 1177 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((ListNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 232:
					// line 1180 "DefaultRubyParser.y"
  {
                    support.commentLastElement(((ListNode)yyVals[-4+yyTop]), new Object[]{yyVals[-3+yyTop]});
                    
                    Node node = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop])});
                    
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((ListNode)yyVals[-4+yyTop]), node);
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 233:
					// line 1188 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 234:
					// line 1192 "DefaultRubyParser.y"
  {
                    support.commentLastElement(((ListNode)yyVals[-4+yyTop]), new Object[]{yyVals[-3+yyTop]});
                    
                    Node node = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop])});
                    
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), node);
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 235:
					// line 1200 "DefaultRubyParser.y"
  {
                    support.commentLastElement(((ListNode)yyVals[-3+yyTop]), new Object[]{yyVals[-2+yyTop]});
                    
                    yyVal = ((ListNode)yyVals[-3+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 236:
					// line 1206 "DefaultRubyParser.y"
  {
                    support.commentLastElement(((ListNode)yyVals[-6+yyTop]), new Object[]{yyVals[-5+yyTop]});
                    support.commentLastElement(((ListNode)yyVals[-4+yyTop]), new Object[]{yyVals[-3+yyTop]});
                    
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
                    
                    Node node = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop])});
                    
		    			yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), ((ListNode)yyVals[-6+yyTop]).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), node);
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 237:
					// line 1217 "DefaultRubyParser.y"
  {
                    Node node = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop])});
                    yyVal = support.arg_blk_pass(new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), node), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 238:
					// line 1221 "DefaultRubyParser.y"
  {
	        }
  break;
case 239:
					// line 1224 "DefaultRubyParser.y"
  {
                      Node node = support.introduceComment(((Node)yyVals[-3+yyTop]), new Object[]{yyVals[-2+yyTop]});
                      yyVal = support.arg_blk_pass(support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))).add(node), ((ListNode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 240:
					// line 1228 "DefaultRubyParser.y"
  {
                      Node node = support.introduceComment(((Node)yyVals[-2+yyTop]), new Object[]{yyVals[-1+yyTop]});
                      yyVal = support.arg_blk_pass(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(node), ((BlockPassNode)yyVals[0+yyTop]));
                  }
  break;
case 241:
					// line 1232 "DefaultRubyParser.y"
  {
                      Node argNode = support.introduceComment(((Node)yyVals[-4+yyTop]), new Object[]{yyVals[-3+yyTop]});
                      Node starNode = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop])});
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))).add(argNode), starNode);
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 242:
					// line 1238 "DefaultRubyParser.y"
  {
                      Node argNode = support.introduceComment(((Node)yyVals[-6+yyTop]), new Object[]{yyVals[-5+yyTop]});
                      Node starNode = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop])});
                      support.commentLastElement(((ListNode)yyVals[-4+yyTop]), new Object[]{yyVals[-3+yyTop]});
                      
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop]))).add(argNode), new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), starNode);
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 243:
					// line 1246 "DefaultRubyParser.y"
  {
                      yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 244:
					// line 1250 "DefaultRubyParser.y"
  {
                      Node node = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop])});
                      support.commentLastElement(((ListNode)yyVals[-4+yyTop]), new Object[]{yyVals[-3+yyTop]});
                      
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), node);
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 245:
					// line 1257 "DefaultRubyParser.y"
  {
                      Node node = support.introduceComment(((Node)yyVals[-3+yyTop]), new Object[]{yyVals[-2+yyTop]});
                      yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop]))).add(node).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 246:
					// line 1262 "DefaultRubyParser.y"
  {
                      Node node = support.introduceComment(((Node)yyVals[-5+yyTop]), new Object[]{yyVals[-4+yyTop]});
                      support.commentLastElement(((ListNode)yyVals[-3+yyTop]), new Object[]{yyVals[-2+yyTop]});
                      
                      yyVal = support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop]))).add(node), ((ListNode)yyVals[-3+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 247:
					// line 1269 "DefaultRubyParser.y"
  {
                      Node arg1Node = support.introduceComment(((Node)yyVals[-6+yyTop]), new Object[]{yyVals[-5+yyTop]});
                      Node arg2Node = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop])});
                      support.commentLastElement(((ListNode)yyVals[-4+yyTop]), new Object[]{yyVals[-3+yyTop]});
                      
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop]))).add(arg1Node).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), arg2Node);
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 248:
					// line 1277 "DefaultRubyParser.y"
  {
                      Node arg1Node = support.introduceComment(((Node)yyVals[-8+yyTop]), new Object[]{yyVals[-7+yyTop]});
                      Node arg2Node = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop])});
                      support.commentLastElement(((ListNode)yyVals[-6+yyTop]), new Object[]{yyVals[-5+yyTop]});       
                      support.commentLastElement(((ListNode)yyVals[-4+yyTop]), new Object[]{yyVals[-3+yyTop]});             
                      
                      yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-8+yyTop])), support.list_concat(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-8+yyTop]))).add(arg1Node), ((ListNode)yyVals[-6+yyTop])).add(new HashNode(getPosition(null), ((ListNode)yyVals[-4+yyTop]))), arg2Node);
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 249:
					// line 1286 "DefaultRubyParser.y"
  {
                      Node node = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop])});
                      
                      yyVal = support.arg_blk_pass(new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), node), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 250:
					// line 1291 "DefaultRubyParser.y"
  {}
  break;
case 251:
					// line 1293 "DefaultRubyParser.y"
  { 
		    yyVal = new Long(lexer.getCmdArgumentState().begin());
		}
  break;
case 252:
					// line 1295 "DefaultRubyParser.y"
  {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 254:
					// line 1301 "DefaultRubyParser.y"
  {                    
		    lexer.setState(LexState.EXPR_ENDARG);
		  }
  break;
case 255:
					// line 1303 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), "don't put space before argument parentheses");
		    yyVal = null;
		  }
  break;
case 256:
					// line 1307 "DefaultRubyParser.y"
  {
		    lexer.setState(LexState.EXPR_ENDARG);
		  }
  break;
case 257:
					// line 1309 "DefaultRubyParser.y"
  {
                    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), "don't put space before argument parentheses");
		    yyVal = support.introduceComment(((Node)yyVals[-2+yyTop]), new Object[]{((Token)yyVals[-3+yyTop]), yyVals[0+yyTop]});
		  }
  break;
case 258:
					// line 1314 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    Node node = new BlockPassNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 259:
					// line 1320 "DefaultRubyParser.y"
  {
					Node bodyNode = support.introduceComment(((BlockPassNode)yyVals[0+yyTop]).getBodyNode(), new Object[]{yyVals[-1+yyTop]});
                    BlockPassNode node = new BlockPassNode(((BlockPassNode)yyVals[0+yyTop]).getPosition(), bodyNode);
                    node.setIterNode(((BlockPassNode)yyVals[0+yyTop]).getIterNode());
                    node.setArgsNode(((BlockPassNode)yyVals[0+yyTop]).getArgsNode());
                    yyVal = node;
                }
  break;
case 261:
					// line 1329 "DefaultRubyParser.y"
  {
				yyVal = new ArrayNode(((Node)yyVals[0+yyTop]).getPosition()).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 262:
					// line 1332 "DefaultRubyParser.y"
  {
              		support.commentLastElement(((ListNode)yyVals[-2+yyTop]), new Object[]{yyVals[-1+yyTop]});
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 263:
					// line 1337 "DefaultRubyParser.y"
  {
		    			support.commentLastElement(((ListNode)yyVals[-2+yyTop]), new Object[]{yyVals[-1+yyTop]});
		    			yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 264:
					// line 1341 "DefaultRubyParser.y"
  {
                    support.commentLastElement(((ListNode)yyVals[-3+yyTop]), new Object[]{yyVals[-2+yyTop]});
                    Node node = support.introduceComment(((Node)yyVals[0+yyTop]), new Object[]{((Token)yyVals[-1+yyTop])});
                    yyVal = support.arg_concat(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((ListNode)yyVals[-3+yyTop]), node);
		}
  break;
case 265:
					// line 1346 "DefaultRubyParser.y"
  { 
              		Node node = support.introduceComment(((Node)yyVals[0+yyTop]), new Object[]{((Token)yyVals[-1+yyTop])}); 
                    yyVal = new SplatNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), node);
		}
  break;
case 274:
					// line 1359 "DefaultRubyParser.y"
  {
                    VCallNode node = new VCallNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
		}
  break;
case 275:
					// line 1364 "DefaultRubyParser.y"
  {
					BeginNode node = new BeginNode(support.union(((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop])});
		}
  break;
case 276:
					// line 1368 "DefaultRubyParser.y"
  { lexer.setState(LexState.EXPR_ENDARG); }
  break;
case 277:
					// line 1368 "DefaultRubyParser.y"
  {
		    warnings.warn(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), "(...) interpreted as grouped expression");
                    yyVal = support.introduceComment(((Node)yyVals[-3+yyTop]), new Object[]{((Token)yyVals[-4+yyTop]),yyVals[-2+yyTop], yyVals[-1+yyTop]});
		}
  break;
case 278:
					// line 1372 "DefaultRubyParser.y"
  {
	            		yyVal = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop]), yyVals[0+yyTop]});
                }
  break;
case 279:
					// line 1375 "DefaultRubyParser.y"
  {
                    Colon2Node node = new Colon2Node(support.union(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 280:
					// line 1379 "DefaultRubyParser.y"
  {
                    Colon3Node node = new Colon3Node(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 281:
					// line 1383 "DefaultRubyParser.y"
  {
                    CallNode node = new CallNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), "[]", ((Node)yyVals[-1+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{yyVals[-2+yyTop],yyVals[0+yyTop]});
                }
  break;
case 282:
					// line 1387 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyVal=  new ZArrayNode(((Token)yyVals[-2+yyTop]).getPosition()); /* zero length array*/

                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop]);
                    }
                    yyVal = support.introduceComment(((Node)yyVal), new Object[]{((Token)yyVals[-2+yyTop]), yyVals[0+yyTop]});
                }
  break;
case 283:
					// line 1396 "DefaultRubyParser.y"
  {
                   	HashNode node = new HashNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((ListNode)yyVals[-1+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop]), yyVals[0+yyTop]});
                }
  break;
case 284:
					// line 1400 "DefaultRubyParser.y"
  {
		    			ReturnNode node = new ReturnNode(((Token)yyVals[0+yyTop]).getPosition(), null);
		    			yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 285:
					// line 1404 "DefaultRubyParser.y"
  {
                    Node node = support.new_yield(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-3+yyTop]), ((Token)yyVals[-2+yyTop]), yyVals[0+yyTop]});
                }
  break;
case 286:
					// line 1408 "DefaultRubyParser.y"
  {
                    YieldNode node = new YieldNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), null, false);
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop]), yyVals[0+yyTop]});
                }
  break;
case 287:
					// line 1412 "DefaultRubyParser.y"
  {
                    YieldNode node = new YieldNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, false);
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 288:
					// line 1416 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
				}
  break;
case 289:
					// line 1418 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                   	DefinedNode node = new DefinedNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((Node)yyVals[-1+yyTop])); 
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-5+yyTop]), yyVals[-4+yyTop], ((Token)yyVals[-3+yyTop]), yyVals[0+yyTop]});
                }
  break;
case 290:
					// line 1423 "DefaultRubyParser.y"
  {
                    ((IterNode)yyVals[0+yyTop]).setIterNode(new FCallNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]), true), (String) ((Token)yyVals[-1+yyTop]).getValue(), null));
                    yyVal = support.introduceComment(((IterNode)yyVals[0+yyTop]), new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 292:
					// line 1428 "DefaultRubyParser.y"
  {
		    if (((Node)yyVals[-1+yyTop]) != null && ((Node)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                        throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), "Both block arg and actual block given.");
		    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 293:
					// line 1435 "DefaultRubyParser.y"
  {
              	IfNode node = new IfNode(support.union(((ISourcePositionHolder)yyVals[-5+yyTop]).getPosition(), getPosition(((ISourcePositionHolder)yyVals[-5+yyTop]))), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
              	yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-5+yyTop]), yyVals[-3+yyTop], ((Node)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
		            NODE *tmp = $$->nd_body;
		            $$->nd_body = $$->nd_else;
		            $$->nd_else = tmp;
			    } */
                }
  break;
case 294:
					// line 1445 "DefaultRubyParser.y"
  {
                    IfNode node = new IfNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-5+yyTop]), yyVals[-3+yyTop], ((Node)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
		            NODE *tmp = $$->nd_body;
		            $$->nd_body = $$->nd_else;
		            $$->nd_else = tmp;
			    } */
                }
  break;
case 295:
					// line 1455 "DefaultRubyParser.y"
  { 
	            lexer.getConditionState().begin();
					}
  break;
case 296:
					// line 1457 "DefaultRubyParser.y"
  {
					    lexer.getConditionState().end();
					}
  break;
case 297:
					// line 1459 "DefaultRubyParser.y"
  {
                   	WhileNode node = new WhileNode(support.union(((Token)yyVals[-6+yyTop]).getPosition(), getPosition(((ISourcePositionHolder)yyVals[-6+yyTop]))), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
					yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-6+yyTop]), yyVals[-3+yyTop], ((Token)yyVals[0+yyTop])});
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_UNTIL);
			    } */
                }
  break;
case 298:
					// line 1467 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 299:
					// line 1469 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 300:
					// line 1471 "DefaultRubyParser.y"
  {
                    UntilNode node = new UntilNode(getPosition(((ISourcePositionHolder)yyVals[-6+yyTop])), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-6+yyTop]), yyVals[-3+yyTop], ((Token)yyVals[0+yyTop])});
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_WHILE);
			    } */
                }
  break;
case 301:
					// line 1481 "DefaultRubyParser.y"
  {
		    			CaseNode node = new CaseNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop])); /* XXX*/
		    			yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-4+yyTop]), yyVals[-2+yyTop], ((Token)yyVals[0+yyTop])});
                }
  break;
case 302:
					// line 1485 "DefaultRubyParser.y"
  {
                    CaseNode node = new CaseNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), null, ((Node)yyVals[-1+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-3+yyTop]), yyVals[-2+yyTop], ((Token)yyVals[0+yyTop])});
                }
  break;
case 303:
					// line 1489 "DefaultRubyParser.y"
  {
		    			
		    			yyVal = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-4+yyTop]), yyVals[-3+yyTop], ((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 304:
					// line 1493 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 305:
					// line 1495 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 306:
					// line 1498 "DefaultRubyParser.y"
  {
                    ForNode node = new ForNode(getPosition(((ISourcePositionHolder)yyVals[-8+yyTop])), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-8+yyTop]), ((Token)yyVals[-6+yyTop]), yyVals[-3+yyTop], ((Token)yyVals[0+yyTop])});
                }
  break;
case 307:
					// line 1502 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("class definition in method body");
                    }
                    support.getLocalNames().push(new LocalNamesElement());
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 308:
					// line 1509 "DefaultRubyParser.y"
  {		  
		  			Object[] tokens = new Object[]{yyVals[-3+yyTop]};
		  			
		  			ClassNode classNode;
		  			
		  			if(((Node)yyVals[-4+yyTop]) == null){
		  				classNode = new ClassNode(support.union(((Token)yyVals[-6+yyTop]), ((Token)yyVals[0+yyTop])), support.introduceComment(((Node)yyVals[-5+yyTop]), tokens), new ScopeNode(getRealPosition(((Node)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), ((Node)yyVals[-1+yyTop])), ((Node)yyVals[-4+yyTop]));
                   
		  			} else {
		  				classNode = new ClassNode(support.union(((Token)yyVals[-6+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-5+yyTop]), new ScopeNode(getRealPosition(((Node)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), ((Node)yyVals[-1+yyTop])), support.introduceComment(((Node)yyVals[-4+yyTop]), tokens));
                   
		  			}
		  			
                     
					tokens = new Object[]{((Token)yyVals[-6+yyTop]),((Token)yyVals[0+yyTop])};
                    yyVal = support.introduceComment(classNode, tokens);
                    /* $<Node>$.setLine($<Integer>4.intValue());*/
                    support.getLocalNames().pop();
                }
  break;
case 309:
					// line 1528 "DefaultRubyParser.y"
  {
                    yyVal = new Boolean(support.isInDef());
                    support.setInDef(false);
                }
  break;
case 310:
					// line 1531 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(support.getInSingle());
                    support.setInSingle(0);
                    support.getLocalNames().push(new LocalNamesElement());
                }
  break;
case 311:
					// line 1536 "DefaultRubyParser.y"
  {
                    SClassNode node = new SClassNode(support.union(((Token)yyVals[-7+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-5+yyTop]), new ScopeNode(getRealPosition(((Node)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), ((Node)yyVals[-1+yyTop])));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-7+yyTop]), ((Token)yyVals[-6+yyTop]), yyVals[-3+yyTop], ((Token)yyVals[0+yyTop])});
                    support.getLocalNames().pop();
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
                }
  break;
case 312:
					// line 1543 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) { 
                        yyerror("module definition in method body");
                    }
                    support.getLocalNames().push(new LocalNamesElement());
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 313:
					// line 1550 "DefaultRubyParser.y"
  {
                    ModuleNode node = new ModuleNode(support.union(((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-3+yyTop]), new ScopeNode(getRealPosition(((Node)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), ((Node)yyVals[-1+yyTop])));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])});
                    /* $<Node>$.setLine($<Integer>3.intValue());*/
                    support.getLocalNames().pop();
                }
  break;
case 314:
					// line 1556 "DefaultRubyParser.y"
  {
		      /* missing
			$<id>$ = cur_mid;
			cur_mid = $2; */
                    support.setInDef(true);
                    support.getLocalNames().push(new LocalNamesElement());
                }
  break;
case 315:
					// line 1564 "DefaultRubyParser.y"
  {
		      /* was in old jruby grammar support.getClassNest() !=0 || IdUtil.isAttrSet($2) ? Visibility.PUBLIC : Visibility.PRIVATE); */
                    /* NOEX_PRIVATE for toplevel */
                    
                    Object[] tokens = new Object[]{((Token)yyVals[-5+yyTop]), ((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])};
                    
                    DefnNode node = new DefnNode(support.union(((Token)yyVals[-5+yyTop]), ((Token)yyVals[0+yyTop])), new ArgumentNode(((ISourcePositionHolder)yyVals[-4+yyTop]).getPosition(), (String) ((Token)yyVals[-4+yyTop]).getValue()), ((Node)yyVals[-2+yyTop]),
		                      new ScopeNode(getRealPosition(((Node)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), ((Node)yyVals[-1+yyTop])), Visibility.PRIVATE);
                    
                    
                    yyVal = support.introduceComment(node, tokens);
                    /* $<Node>$.setPosFrom($4);*/
                    support.getLocalNames().pop();
                    support.setInDef(false);
		    /* missing cur_mid = $<id>3; */
                }
  break;
case 316:
					// line 1580 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 317:
					// line 1582 "DefaultRubyParser.y"
  {
                    support.setInSingle(support.getInSingle() + 1);
                    support.getLocalNames().push(new LocalNamesElement());
                    lexer.setState(LexState.EXPR_END); /* force for args */
                }
  break;
case 318:
					// line 1588 "DefaultRubyParser.y"
  {
                  	DefsNode node = new DefsNode(support.union(((Token)yyVals[-8+yyTop]), ((Token)yyVals[0+yyTop])), ((Node)yyVals[-7+yyTop]), (String) ((Token)yyVals[-4+yyTop]).getValue(), ((Node)yyVals[-2+yyTop]), new ScopeNode(getPosition(null), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), ((Node)yyVals[-1+yyTop])));
                    
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-8+yyTop]), ((Token)yyVals[-6+yyTop]), ((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])});
					/* $<Node>$.setPosFrom($2);*/
                    support.getLocalNames().pop();
                    support.setInSingle(support.getInSingle() - 1);
                }
  break;
case 319:
					// line 1596 "DefaultRubyParser.y"
  {
                    BreakNode node = new BreakNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 320:
					// line 1600 "DefaultRubyParser.y"
  {
                    NextNode node = new NextNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 321:
					// line 1604 "DefaultRubyParser.y"
  {
              		RedoNode node = new RedoNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 322:
					// line 1608 "DefaultRubyParser.y"
  {
                    RetryNode node = new RetryNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 323:
					// line 1613 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
		    yyVal = ((Node)yyVals[0+yyTop]);
		}
  break;
case 327:
					// line 1621 "DefaultRubyParser.y"
  {
              	    ((Token)yyVals[-1+yyTop]).addComments(((Token)yyVals[0+yyTop]).getComments());
              }
  break;
case 332:
					// line 1632 "DefaultRubyParser.y"
  {
               		IfNode node = new IfNode(support.union(((ISourcePositionHolder)yyVals[-4+yyTop]).getPosition(), getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
               		yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-4+yyTop]), yyVals[-2+yyTop]});
                }
  break;
case 334:
					// line 1638 "DefaultRubyParser.y"
  {
                    yyVal = support.introduceComment(((Node)yyVals[0+yyTop]), new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 336:
					// line 1643 "DefaultRubyParser.y"
  {}
  break;
case 338:
					// line 1646 "DefaultRubyParser.y"
  {
                    ZeroArgNode node = new ZeroArgNode(getPosition(null));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 339:
					// line 1650 "DefaultRubyParser.y"
  {
                    ZeroArgNode node = new ZeroArgNode(getPosition(null));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
		}
  break;
case 340:
					// line 1654 "DefaultRubyParser.y"
  {
                    yyVal = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 341:
					// line 1658 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push(new BlockNamesElement());
		}
  break;
case 342:
					// line 1661 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])}), null);
                   
                    support.getBlockNames().pop();
                }
  break;
case 343:
					// line 1667 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                        throw new SyntaxException(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), "Both block arg and actual block given.");
                    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 344:
					// line 1674 "DefaultRubyParser.y"
  {
                    Node node = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop])});
                }
  break;
case 345:
					// line 1678 "DefaultRubyParser.y"
  {
                    Node node = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop])});
                }
  break;
case 346:
					// line 1683 "DefaultRubyParser.y"
  {
                    Node node = support.new_fcall((String) ((Token)yyVals[-1+yyTop]).getValue(), ((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop])); /* .setPosFrom($2);*/
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 347:
					// line 1687 "DefaultRubyParser.y"
  {
                    Node node = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop])});
                }
  break;
case 348:
					// line 1691 "DefaultRubyParser.y"
  {
                    Node node = support.new_call(((Node)yyVals[-3+yyTop]), ((Token)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[-1+yyTop])});
                }
  break;
case 349:
					// line 1695 "DefaultRubyParser.y"
  {
              		Node node = support.new_call(((Node)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop]), null);
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 350:
					// line 1699 "DefaultRubyParser.y"
  {
              		Node node = support.new_super(((Node)yyVals[0+yyTop]), ((Token)yyVals[-1+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 351:
					// line 1703 "DefaultRubyParser.y"
  {
                    	ZSuperNode node = new ZSuperNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 352:
					// line 1708 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push(new BlockNamesElement());
		}
  break;
case 353:
					// line 1710 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-4+yyTop]), yyVals[0+yyTop]}), null);
                    support.getBlockNames().pop();
                }
  break;
case 354:
					// line 1714 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push(new BlockNamesElement());
		}
  break;
case 355:
					// line 1716 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(support.union(((Token)yyVals[-4+yyTop]).getPosition(), getPosition(((ISourcePositionHolder)yyVals[-4+yyTop]))), ((Node)yyVals[-2+yyTop]), support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-4+yyTop]), ((Token)yyVals[0+yyTop])}), null);
                    support.getBlockNames().pop();
                }
  break;
case 356:
					// line 1723 "DefaultRubyParser.y"
  {
		    			WhenNode node = new WhenNode(getPosition(((ISourcePositionHolder)yyVals[-4+yyTop])), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		    			yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-4+yyTop]), yyVals[-2+yyTop]});
                }
  break;
case 358:
					// line 1729 "DefaultRubyParser.y"
  {
                    support.commentLastElement(((ListNode)yyVals[-3+yyTop]), new Object[]{yyVals[-2+yyTop]});
                    yyVal = ((ListNode)yyVals[-3+yyTop]).add(new WhenNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), support.introduceComment(((Node)yyVals[0+yyTop]), new Object[]{((Token)yyVals[-1+yyTop])}), null, null));
                }
  break;
case 359:
					// line 1733 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]))).add(new WhenNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), support.introduceComment(((Node)yyVals[0+yyTop]), new Object[]{((Token)yyVals[-1+yyTop])}), null, null));
                }
  break;
case 362:
					// line 1743 "DefaultRubyParser.y"
  {
                    Node node;
		    if (((Node)yyVals[-3+yyTop]) != null) {
                       node = support.appendToBlock(support.node_assign(((Node)yyVals[-3+yyTop]), new GlobalVarNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), "$!")), support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-5+yyTop]), yyVals[-2+yyTop]}));
		    } else {
		       node = ((Node)yyVals[-1+yyTop]);
                    }
                    yyVal = new RescueBodyNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop]), true), ((Node)yyVals[-4+yyTop]), node, ((RescueBodyNode)yyVals[0+yyTop]));
		}
  break;
case 363:
					// line 1752 "DefaultRubyParser.y"
  {yyVal = null;}
  break;
case 364:
					// line 1754 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition()).add(((Node)yyVals[0+yyTop]));
		}
  break;
case 367:
					// line 1760 "DefaultRubyParser.y"
  {
                    yyVal = support.introduceComment(((Node)yyVals[0+yyTop]), new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 369:
					// line 1765 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]) != null) {
                        yyVal = ((Node)yyVals[0+yyTop]);
                    } else {
                        yyVal = new NilNode(getPosition(null));
                    }
                    yyVal = support.introduceComment(((Node)yyVal), new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 372:
					// line 1776 "DefaultRubyParser.y"
  {
              		SymbolNode node = new SymbolNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 374:
					// line 1782 "DefaultRubyParser.y"
  {
		    if (((Node)yyVals[0+yyTop]) == null) {
		        yyVal = new StrNode(getPosition(((Node)yyVals[0+yyTop])), "");
		    } else {
		        if (((Node)yyVals[0+yyTop]) instanceof EvStrNode) {
			    yyVal = new DStrNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
			} else {
		            yyVal = ((Node)yyVals[0+yyTop]);
			}
		    }
		}
  break;
case 375:
					// line 1794 "DefaultRubyParser.y"
  {
                    yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, ((Node)yyVals[0+yyTop]));
		}
  break;
case 376:
					// line 1797 "DefaultRubyParser.y"
  {
                    yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		}
  break;
case 377:
					// line 1801 "DefaultRubyParser.y"
  {
		     yyVal = support.introduceComment(((Node)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])});
		}
  break;
case 378:
					// line 1805 "DefaultRubyParser.y"
  {
		    if (((Node)yyVals[-1+yyTop]) == null) {
			  yyVal = new XStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])),null);
		    } else {
		      if (((Node)yyVals[-1+yyTop]) instanceof StrNode) {
			  yyVal = new XStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((StrNode)yyVals[-1+yyTop]).getValue());
		      } else if (((Node)yyVals[-1+yyTop]) instanceof DStrNode) {
			  yyVal = new DXStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((Node)yyVals[-1+yyTop]));
		      } else {
					yyVal = new DXStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(((Node)yyVals[-1+yyTop])));
		      }
		    }
		    		yyVal = support.introduceComment(((Node)yyVal), new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])});
                }
  break;
case 379:
					// line 1820 "DefaultRubyParser.y"
  {
		    int options = ((RegexpNode)((Token)yyVals[0+yyTop]).getValue()).getOptions();
		    Node node = ((Node)yyVals[-1+yyTop]);

		    if (node == null) {
		        yyVal = new RegexpNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), "", options & ~ReOptions.RE_OPTION_ONCE);
		    } else if (node instanceof StrNode) {
		      yyVal = new RegexpNode(((Node)yyVals[-1+yyTop]).getPosition(), ((StrNode) node).getValue(), options & ~ReOptions.RE_OPTION_ONCE);
		    } else {
		        if (node instanceof DStrNode == false) {
			    node = new DStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(node));
		        } 

			yyVal = new DRegexpNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), options, (options & ReOptions.RE_OPTION_ONCE) != 0).add(node);
		    }
		    yyVal = support.introduceComment(((Node)yyVal), new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])});
		 }
  break;
case 380:
					// line 1838 "DefaultRubyParser.y"
  {
		     ZArrayNode node = new ZArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])));
		     yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop]), yyVals[-1+yyTop], ((Token)yyVals[0+yyTop])});
		 }
  break;
case 381:
					// line 1842 "DefaultRubyParser.y"
  {
		     yyVal = support.introduceComment(((ListNode)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])});
		 }
  break;
case 382:
					// line 1846 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 383:
					// line 1849 "DefaultRubyParser.y"
  {
                     Node node = ((Node)yyVals[-1+yyTop]);

                     if (node instanceof EvStrNode) {
		       node = new DStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(node);
		     }

		     if (((ListNode)yyVals[-2+yyTop]) == null) {
		       yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(node);
		     } else {
		       yyVal = ((ListNode)yyVals[-2+yyTop]).add(node);
		     }
		 }
  break;
case 385:
					// line 1864 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	       }
  break;
case 386:
					// line 1868 "DefaultRubyParser.y"
  {
		     ZArrayNode node = new ZArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])));
		     yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop]), yyVals[-1+yyTop], ((Token)yyVals[0+yyTop])});
		 }
  break;
case 387:
					// line 1872 "DefaultRubyParser.y"
  {
		     yyVal = support.introduceComment(((ListNode)yyVals[-1+yyTop]), new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])});
		 }
  break;
case 388:
					// line 1876 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 389:
					// line 1879 "DefaultRubyParser.y"
  {
                     if (((ListNode)yyVals[-2+yyTop]) == null) {
		            		yyVal = new ArrayNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop]))).add(new StrNode(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), (String) ((Token)yyVals[-1+yyTop]).getValue()));
		         
		     } else {
		         yyVal = ((ListNode)yyVals[-2+yyTop]).add(new StrNode(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), (String) ((Token)yyVals[-1+yyTop]).getValue()));
		     }
		 }
  break;
case 390:
					// line 1888 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 391:
					// line 1891 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		 }
  break;
case 392:
					// line 1895 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 393:
					// line 1898 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		 }
  break;
case 394:
					// line 1903 "DefaultRubyParser.y"
  {
						StrNode node = new StrNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
                      yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                  }
  break;
case 395:
					// line 1907 "DefaultRubyParser.y"
  {
                      yyVal = lexer.getStrTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  }
  break;
case 396:
					// line 1911 "DefaultRubyParser.y"
  {
		      lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
		      EvStrNode node = new EvStrNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
		      yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop])});
		  }
  break;
case 397:
					// line 1916 "DefaultRubyParser.y"
  {
		      yyVal = lexer.getStrTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  }
  break;
case 398:
					// line 1920 "DefaultRubyParser.y"
  {
		      lexer.setStrTerm(((StrTerm)yyVals[-2+yyTop]));
		      Node node = ((Node)yyVals[-1+yyTop]);

		      if (node instanceof NewlineNode) {
		        node = ((NewlineNode)node).getNextNode();
		      }
				Node evNode = support.newEvStrNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop]), true), node);
		      yyVal = support.introduceComment(evNode, new Object[]{((Token)yyVals[-3+yyTop]), yyVals[0+yyTop]});
		  }
  break;
case 399:
					// line 1931 "DefaultRubyParser.y"
  {
                      yyVal = new GlobalVarNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                 }
  break;
case 400:
					// line 1934 "DefaultRubyParser.y"
  {
                      yyVal = new InstVarNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                 }
  break;
case 401:
					// line 1937 "DefaultRubyParser.y"
  {
                      yyVal = new ClassVarNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), (String) ((Token)yyVals[0+yyTop]).getValue());
                 }
  break;
case 403:
					// line 1943 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    ((Token)yyVals[0+yyTop]).addComments(((Token)yyVals[-1+yyTop]).getComments());
                    yyVal = ((Token)yyVals[0+yyTop]);
		    			((ISourcePositionHolder)yyVal).setPosition(support.union(((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])));
                }
  break;
case 408:
					// line 1955 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);

		    /* In ruby, it seems to be possible to get a*/
		    /* StrNode (NODE_STR) among other node type.  This */
		    /* is not possible for us.  We will always have a */
		    /* DStrNode (NODE_DSTR).*/
		    if(((Node)yyVals[-1+yyTop]) instanceof StrNode){
		    	DStrNode n = new DStrNode(((StrNode)yyVals[-1+yyTop]).getPosition());
			n.add(((StrNode)yyVals[-1+yyTop]));
			yyVal = new DSymbolNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), n);
		    }else{
		    		yyVal = new DSymbolNode(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), ((DStrNode)yyVals[-1+yyTop]));
		    	}
		    	yyVal = support.introduceComment(((Node)yyVal), new Object[]{((Token)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])});
		}
  break;
case 409:
					// line 1972 "DefaultRubyParser.y"
  {
                    Object number = ((Token)yyVals[0+yyTop]).getValue();

                    if (number instanceof Long) {
		        yyVal = new FixnumNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), ((Long) number).longValue());
                    } else {
		        yyVal = new BignumNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (BigInteger) number);
                    }
                    yyVal = support.introduceComment(((Node)yyVal), new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 410:
					// line 1982 "DefaultRubyParser.y"
  {
                    FloatNode node = new FloatNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), ((Double) ((Token)yyVals[0+yyTop]).getValue()).doubleValue());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
	        }
  break;
case 411:
					// line 1986 "DefaultRubyParser.y"
  {
                    Object number = ((Token)yyVals[0+yyTop]).getValue();
					Node node = support.getOperatorCallNode((number instanceof Long ? (Node) new FixnumNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Long) number).longValue()) : (Node) new BignumNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), (BigInteger) number)), "-@");
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
		}
  break;
case 412:
					// line 1991 "DefaultRubyParser.y"
  {
                    Node node = support.getOperatorCallNode(new FloatNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((Double) ((Token)yyVals[0+yyTop]).getValue()).doubleValue()), "-@");
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop]), ((Token)yyVals[0+yyTop])});
		}
  break;
case 413:
					// line 2001 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 414:
					// line 2004 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 415:
					// line 2007 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 416:
					// line 2010 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 417:
					// line 2013 "DefaultRubyParser.y"
  {
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 418:
					// line 2016 "DefaultRubyParser.y"
  { 
              		NilNode node = new NilNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 419:
					// line 2020 "DefaultRubyParser.y"
  {
              		SelfNode node = new SelfNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 420:
					// line 2024 "DefaultRubyParser.y"
  { 
              		TrueNode node = new TrueNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
		    			yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 421:
					// line 2028 "DefaultRubyParser.y"
  {
              		FalseNode node = new FalseNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
		    			yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 422:
					// line 2032 "DefaultRubyParser.y"
  {
                    StrNode node = new StrNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), getPosition(((ISourcePositionHolder)yyVals[0+yyTop])).getFile());
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])}); 
                    
                }
  break;
case 423:
					// line 2037 "DefaultRubyParser.y"
  {
              		FixnumNode node = new FixnumNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), getPosition(((ISourcePositionHolder)yyVals[0+yyTop])).getEndLine() + 1);
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])});
                }
  break;
case 424:
					// line 2042 "DefaultRubyParser.y"
  {
                    /* Work around __LINE__ and __FILE__ */
                    if (yyVals[0+yyTop] instanceof INameNode) {
		        			String name = ((INameNode)yyVals[0+yyTop]).getName();
                        yyVal = support.gettable(name, ((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
				    } else if (yyVals[0+yyTop] instanceof Token) {
				      yyVal = support.gettable((String) ((Token)yyVals[0+yyTop]).getValue(), ((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
				    } else {
				        yyVal = yyVals[0+yyTop];
				    }
				    yyVal = support.introduceComment(((Node)yyVal), new Object[]{yyVals[0+yyTop]});
                }
  break;
case 425:
					// line 2056 "DefaultRubyParser.y"
  {
                    Node node = support.assignable(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), yyVals[0+yyTop], null);
                    yyVal = support.introduceComment(node, new Object[]{yyVals[0+yyTop]});
                }
  break;
case 426:
					// line 2062 "DefaultRubyParser.y"
  {
					yyVal = support.introduceComment((Node)((Token)yyVals[0+yyTop]).getValue(), new Object[]{((Token)yyVals[0+yyTop])});
					
				}
  break;
case 427:
					// line 2067 "DefaultRubyParser.y"
  {
					yyVal = support.introduceComment((Node)((Token)yyVals[0+yyTop]).getValue(), new Object[]{((Token)yyVals[0+yyTop])});
				}
  break;
case 428:
					// line 2071 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 429:
					// line 2074 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 430:
					// line 2076 "DefaultRubyParser.y"
  {
                		
                    yyVal = support.introduceComment(((Node)yyVals[0+yyTop]), new Object[]{((Token)yyVals[-2+yyTop])});
                }
  break;
case 431:
					// line 2080 "DefaultRubyParser.y"
  {
                    yyerrok();
                    yyVal = null;
                }
  break;
case 432:
					// line 2085 "DefaultRubyParser.y"
  {
                    Object[] tokens = new Object[]{((Token)yyVals[-3+yyTop]), yyVals[-1+yyTop] ,yyVals[0+yyTop]};
                                                            
                    yyVal = support.introduceComment(((Node)yyVals[-2+yyTop]), tokens);
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 433:
					// line 2091 "DefaultRubyParser.y"
  {
              		Object[] tokens = new Object[]{yyVals[0+yyTop]};
              		yyVal = support.introduceComment(((Node)yyVals[-1+yyTop]), tokens);
                }
  break;
case 434:
					// line 2096 "DefaultRubyParser.y"
  {
                    ListNode argLNode = support.commentLastElement(((ListNode)yyVals[-5+yyTop]), new Object[]{yyVals[-4+yyTop]});
                    ListNode optLNode = support.commentLastElement(((ListNode)yyVals[-3+yyTop]), new Object[]{yyVals[-2+yyTop]});
                    ArgsNode node = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-5+yyTop])), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                    
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-1+yyTop])});
                }
  break;
case 435:
					// line 2103 "DefaultRubyParser.y"
  {
                    Object[] tokens = new Object[]{yyVals[-2+yyTop]};
                    ListNode lNode = support.commentLastElement(((ListNode)yyVals[-3+yyTop]), tokens);
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), lNode, ((ListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 436:
					// line 2110 "DefaultRubyParser.y"
  {
                    Object[] tokens = new Object[]{yyVals[-2+yyTop]};
                    ListNode lNode = support.commentLastElement(((ListNode)yyVals[-3+yyTop]), tokens);
                    
                    tokens = new Object[]{((Token)yyVals[-1+yyTop])};
                    ArgsNode node = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), lNode, null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                     
                    yyVal = support.introduceComment(node, tokens);
                    }
  break;
case 437:
					// line 2120 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(((ISourcePositionHolder)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 438:
					// line 2123 "DefaultRubyParser.y"
  {
                    Object[] tokens = new Object[]{yyVals[-2+yyTop]};
                    
                    ListNode lNode = support.commentLastElement(((ListNode)yyVals[-3+yyTop]),tokens);
                    
                    tokens = new Object[]{((Token)yyVals[-1+yyTop])};                    
                    ArgsNode node = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-3+yyTop])), null, lNode, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, tokens);
                }
  break;
case 439:
					// line 2132 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 440:
					// line 2135 "DefaultRubyParser.y"
  {
             		Object[] tokens = new Object[]{((Token)yyVals[-1+yyTop])};
             		ArgsNode node = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), null, null, ((Integer) ((Token)yyVals[-1+yyTop]).getValue()).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, tokens);
                      
                    }
  break;
case 441:
					// line 2141 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop])), null, null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 442:
					// line 2144 "DefaultRubyParser.y"
  {
	      	   /*take the last position from the lexer, this isn't entirely correct, but more correct then getPosition(null).*/
                   yyVal = new ArgsNode(lexer.getPosition(), null, null, -1, null);
                }
  break;
case 443:
					// line 2149 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a constant");
                }
  break;
case 444:
					// line 2152 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be an instance variable");
                }
  break;
case 445:
					// line 2155 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a class variable");
                }
  break;
case 446:
					// line 2158 "DefaultRubyParser.y"
  {
                   String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();
                   if (!IdUtil.isLocal(identifier)) {
                        yyerror("formal argument must be local variable");
                    } else if (((LocalNamesElement) support.getLocalNames().peek()).isLocalRegistered(identifier)) {
                        yyerror("duplicate argument name");
                    }
		    /* Register new local var or die trying (side-effect)*/
                    ((LocalNamesElement) support.getLocalNames().peek()).getLocalIndex(identifier);
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 447:
					// line 2170 "DefaultRubyParser.y"
  {
					ArgumentNode node = new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
                    yyVal = new ListNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition());
                    ((ListNode) yyVal).add(support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])}));
                }
  break;
case 448:
					// line 2175 "DefaultRubyParser.y"
  {
                    ArgumentNode node = new ArgumentNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition(), (String) ((Token)yyVals[0+yyTop]).getValue());
                    
                    support.commentLastElement(((ListNode)yyVals[-2+yyTop]), new Object[]{yyVals[-1+yyTop]});
                    
                    ((ListNode)yyVals[-2+yyTop]).add(support.introduceComment(node, new Object[]{((Token)yyVals[0+yyTop])}));
                    ((ListNode)yyVals[-2+yyTop]).setPosition(support.union(((ListNode)yyVals[-2+yyTop]), ((Token)yyVals[0+yyTop])));
		    yyVal = ((ListNode)yyVals[-2+yyTop]);
                }
  break;
case 449:
					// line 2185 "DefaultRubyParser.y"
  {
                    String identifier = (String) ((Token)yyVals[-2+yyTop]).getValue();

                    if (!IdUtil.isLocal(identifier)) {
                        yyerror("formal argument must be local variable");
                    } else if (((LocalNamesElement) support.getLocalNames().peek()).isLocalRegistered(identifier)) {
                        yyerror("duplicate optional argument name");
                    }
		    ((LocalNamesElement) support.getLocalNames().peek()).getLocalIndex(identifier);
                    
                    Node node = support.assignable(getPosition(((ISourcePositionHolder)yyVals[-2+yyTop])), identifier, ((Node)yyVals[0+yyTop]));
                    yyVal = support.introduceComment(node, new Object[]{((Token)yyVals[-2+yyTop]), yyVals[-1+yyTop]});
                }
  break;
case 450:
					// line 2199 "DefaultRubyParser.y"
  {
                    yyVal = new BlockNode(getPosition(((ISourcePositionHolder)yyVals[0+yyTop]))).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 451:
					// line 2202 "DefaultRubyParser.y"
  {
                    support.commentLastElement(((ListNode)yyVals[-2+yyTop]), new Object[]{yyVals[-1+yyTop]});
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 454:
					// line 2210 "DefaultRubyParser.y"
  {
                    String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                    if (!IdUtil.isLocal(identifier)) {
                        yyerror("rest argument must be local variable");
                    } else if (((LocalNamesElement) support.getLocalNames().peek()).isLocalRegistered(identifier)) {
                        yyerror("duplicate rest argument name");
                    }
		    ((Token)yyVals[-1+yyTop]).setValue(new Integer(((LocalNamesElement) support.getLocalNames().peek()).getLocalIndex(identifier)));
                    yyVal = ((Token)yyVals[-1+yyTop]);
                }
  break;
case 455:
					// line 2221 "DefaultRubyParser.y"
  {
                    ((Token)yyVals[0+yyTop]).setValue(new Integer(-2));
                    yyVal = ((Token)yyVals[0+yyTop]);
                }
  break;
case 458:
					// line 2229 "DefaultRubyParser.y"
  {
                    String identifier = (String) ((Token)yyVals[0+yyTop]).getValue();

                    if (!IdUtil.isLocal(identifier)) {
                        yyerror("block argument must be local variable");
                    } else if (((LocalNamesElement) support.getLocalNames().peek()).isLocalRegistered(identifier)) {
                        yyerror("duplicate block argument name");
                    }
                    yyVal = new BlockArgNode(getPosition(((ISourcePositionHolder)yyVals[-1+yyTop])), ((LocalNamesElement) support.getLocalNames().peek()).getLocalIndex(identifier));
                }
  break;
case 459:
					// line 2240 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop]);
                }
  break;
case 460:
					// line 2243 "DefaultRubyParser.y"
  {
	            yyVal = null;
	        }
  break;
case 461:
					// line 2247 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]) instanceof SelfNode) {
		        yyVal = new SelfNode(((ISourcePositionHolder)yyVals[0+yyTop]).getPosition()); 
                    } else {
			support.checkExpression(((Node)yyVals[0+yyTop]));
			yyVal = ((Node)yyVals[0+yyTop]);
		    }
                }
  break;
case 462:
					// line 2255 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 463:
					// line 2257 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) instanceof ILiteralNode) {
                        /*case Constants.NODE_STR:
                        case Constants.NODE_DSTR:
                        case Constants.NODE_XSTR:
                        case Constants.NODE_DXSTR:
                        case Constants.NODE_DREGX:
                        case Constants.NODE_LIT:
                        case Constants.NODE_ARRAY:
                        case Constants.NODE_ZARRAY:*/
                        yyerror("Can't define single method for literals.");
                    }
		    support.checkExpression(((Node)yyVals[-2+yyTop]));
		    
		    			yyVal = support.introduceComment(((Node)yyVals[-2+yyTop]), new Object[]{((Token)yyVals[-4+yyTop]), yyVals[-1+yyTop], yyVals[0+yyTop]});
                }
  break;
case 465:
					// line 2275 "DefaultRubyParser.y"
  {
                    yyVal = support.commentLastElement(((ListNode)yyVals[-1+yyTop]), new Object[]{yyVals[0+yyTop]});
                }
  break;
case 466:
					// line 2278 "DefaultRubyParser.y"
  {
                    if (((ListNode)yyVals[-1+yyTop]).size() % 2 != 0) {
                        yyerror("Odd number list for Hash.");
                    }
                    yyVal = support.commentLastElement(((ListNode)yyVals[-1+yyTop]), new Object[]{yyVals[0+yyTop]});
                }
  break;
case 468:
					// line 2286 "DefaultRubyParser.y"
  {
              		support.commentLastElement(((ListNode)yyVals[-2+yyTop]), new Object[]{yyVals[-1+yyTop]});
                    yyVal = ((ListNode)yyVals[-2+yyTop]).addAll(((ListNode)yyVals[0+yyTop]));
                }
  break;
case 469:
					// line 2291 "DefaultRubyParser.y"
  {
					Node node = support.introduceComment(((Node)yyVals[-2+yyTop]), new Object[]{((Token)yyVals[-1+yyTop])});
                    yyVal = new ArrayNode(support.union(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]))).add(node).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 489:
					// line 2322 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 492:
					// line 2328 "DefaultRubyParser.y"
  {
                    yyerrok();
                    ((Token)yyVals[-1+yyTop]).addComments(((Token)yyVals[0+yyTop]).getComments());
                }
  break;
case 493:
					// line 2333 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 494:
					// line 2337 "DefaultRubyParser.y"
  {  yyVal = null;
		  }
  break;
case 495:
					// line 2340 "DefaultRubyParser.y"
  {  yyVal = null;
		  }
  break;
					// line 8043 "-"
        }
        yyTop -= yyLen[yyN];
        yyState = yyStates[yyTop];
        int yyM = yyLhs[yyN];
        if (yyState == 0 && yyM == 0) {
          yyState = yyFinal;
          if (yyToken < 0) {
            yyToken = yyLex.advance() ? yyLex.token() : 0;
          }
          if (yyToken == 0) {
            return yyVal;
          }
          continue yyLoop;
        }
        if ((yyN = yyGindex[yyM]) != 0 && (yyN += yyState) >= 0
            && yyN < yyTable.length && yyCheck[yyN] == yyState)
          yyState = yyTable[yyN];
        else
          yyState = yyDgoto[yyM];
        continue yyLoop;
      }
    }
  }

					// line 2347 "DefaultRubyParser.y"


    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public RubyParserResult parse(LexerSource source) {
        support.reset();
        support.setResult(new RubyParserResult());
        
        lexer.reset();
        lexer.setSource(source);
        support.setPositionFactory(lexer.getPositionFactory());
        try {
	    //yyparse(lexer, new jay.yydebug.yyAnim("JRuby", 9));
	    //yyparse(lexer, new jay.yydebug.yyDebugAdapter());
	    yyparse(lexer, null);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (yyException e) {
            e.printStackTrace();
        }
        
        return support.getResult();
    }

    public void init(RubyParserConfiguration configuration) {
        support.setConfiguration(configuration);
    }

    // +++
    // Helper Methods
    
    void yyerrok() {}

    private ISourcePosition getRealPosition(Node node) {
      if (node == null) {
	return getPosition(null);
      }

      if (node instanceof BlockNode) {
	return node.getPosition();
      }

      if (node instanceof NewlineNode) {
	while (node instanceof NewlineNode) {
	  node = ((NewlineNode) node).getNextNode();
	}
	return node.getPosition();
      }

      return getPosition((ISourcePositionHolder)node);
    }

    private ISourcePosition getPosition(ISourcePositionHolder start) {
        return getPosition(start, false);
    }

    private ISourcePosition getPosition(ISourcePositionHolder start, boolean inclusive) {
        if (start != null) {
	    return lexer.getPosition(start.getPosition(), inclusive);
	} 
	
	return lexer.getPosition(null, inclusive);
    }
}
					// line 8139 "-"
