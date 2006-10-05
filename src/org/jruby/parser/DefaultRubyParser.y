%{
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
%}

%token <Token> kCLASS
    kMODULE
    kDEF
    kUNDEF
    kBEGIN
    kRESCUE
    kENSURE
    kEND
    kIF
    kUNLESS
    kTHEN
    kELSIF
    kELSE
    kCASE
    kWHEN
    kWHILE
    kUNTIL
    kFOR
    kBREAK
    kNEXT
    kREDO
    kRETRY
    kIN
    kDO
    kDO_COND
    kDO_BLOCK
    kRETURN
    kYIELD
    kSUPER
    kSELF
    kNIL
    kTRUE
    kFALSE
    kAND
    kOR
    kNOT
    kIF_MOD
    kUNLESS_MOD
    kWHILE_MOD
    kUNTIL_MOD
    kRESCUE_MOD
    kALIAS
    kDEFINED
    klBEGIN
    klEND
    k__LINE__
    k__FILE__

   // On farg
%token <Token>  tIDENTIFIER tFID tGVAR tIVAR tCONSTANT tCVAR tSTRING_CONTENT
%token <Token>  tINTEGER tFLOAT
%token <Token> tNTH_REF tBACK_REF
%token <Token>    tREGEXP_END

%type <Node>  singleton strings string string1 xstring regexp
%type <Node>  string_contents xstring_contents string_content
%type <Node>  words qwords word
%type <Node>  literal numeric dsym 
%type <Node> cpath
%type <Node>  compstmt bodystmt stmts stmt expr arg primary command command_call method_call
%type <ListNode> qword_list word_list 
%type <Node>  expr_value primary_value opt_else cases
%type <Node>  if_tail exc_var opt_ensure paren_args opt_paren_args
%type <Node>  call_args call_args2 open_args
%type <Node>  command_args var_ref 
%type <BlockPassNode> opt_block_arg block_arg none_block_pass
%type <Node>  superclass block_call block_command
%type <BlockArgNode> opt_f_block_arg f_block_arg 
%type <Node> f_arglist f_args f_opt
%type <Node> undef_list string_dvar backref 
%type <Node> block_var opt_block_var lhs none
%type <IterNode> brace_block do_block cmd_brace_block 
%type <Node> mrhs mlhs_item mlhs_node arg_value case_body 
%type <MultipleAsgnNode> mlhs mlhs_basic mlhs_entry
%type <ListNode> args when_args mlhs_head assocs assoc 
%type <Node> exc_list aref_args 
%type <RescueBodyNode> opt_rescue
%type <Object> variable 
%type <Node> var_lhs
%type <ListNode> none_list assoc_list f_optarg 
%type <Token>   fitem sym symbol operation operation2 operation3
%type <Token>   cname fname op 
%type <Token>  f_norm_arg f_rest_arg
%type <ListNode> f_arg
%type <Token> dot_or_colon
%type <Token> restarg_mark blkarg_mark
%token <Token> tUPLUS         /* unary+ */
%token <Token> tUMINUS        /* unary- */
%token <Token> tUMINUS_NUM    /* unary- */
%token <Token> tPOW           /* ** */
%token <Token> tCMP           /* <=> */
%token <Token> tEQ            /* == */
%token <Token> tEQQ           /* === */
%token <Token> tNEQ           /* != */
%token <Token> tGEQ           /* >= */
%token <Token> tLEQ           /* <= */
%token <Token> tANDOP tOROP   /* && and || */
%token <Token> tMATCH tNMATCH /* =~ and !~ */
%token <Token>  tDOT           /* Is just '.' in ruby and not a token */
%token <Token> tDOT2 tDOT3    /* .. and ... */
%token <Token> tAREF tASET    /* [] and []= */
%token <Token> tLSHFT tRSHFT  /* << and >> */
%token <Token> tCOLON2        /* :: */
%token <Token> tCOLON3        /* :: at EXPR_BEG */
%token <Token> tOP_ASGN       /* +=, -=  etc. */
%token <Token> tASSOC         /* => */
%token <Token> tLPAREN        /* ( */
%token <Token> tLPAREN2        /* ( Is just '(' in ruby and not a token */
%token <Token> tLPAREN_ARG    /* ( */
%token <Token> tLBRACK        /* [ */
%token <Token> tLBRACE        /* { */
%token <Token> tLBRACE_ARG    /* { */
%token <Token> tSTAR          /* * */
%token <Token> tSTAR2         /* *  Is just '*' in ruby and not a token */
%token <Token> tAMPER         /* & */
%token <Token> tAMPER2        /* &  Is just '&' in ruby and not a token */
%token <Token> tTILDE         /* ` is just '`' in ruby and not a token */
%token <Token> tPERCENT       /* % is just '%' in ruby and not a token */
%token <Token> tDIVIDE        /* / is just '/' in ruby and not a token */
%token <Token> tPLUS          /* + is just '+' in ruby and not a token */
%token <Token> tMINUS         /* - is just '-' in ruby and not a token */
%token <Token> tLT            /* < is just '<' in ruby and not a token */
%token <Token> tGT            /* > is just '>' in ruby and not a token */
%token <Token> tPIPE          /* | is just '|' in ruby and not a token */
%token <Token> tBANG          /* ! is just '!' in ruby and not a token */
%token <Token> tCARET         /* ^ is just '^' in ruby and not a token */
%token <Token> tLCURLY        /* { is just '{' in ruby and not a token */
%token <Token> tBACK_REF2     /* { is just '`' in ruby and not a token */
%token <Token> tSYMBEG tSTRING_BEG tXSTRING_BEG tREGEXP_BEG tWORDS_BEG tQWORDS_BEG
%token <Token> tSTRING_DBEG tSTRING_DVAR tSTRING_END


//tcorbat: Defining comment tokens, only for the lexer

%token <Token> tTAILCOMMENT
%token <Token> tSOLOCOMMENT
%token <Token> tEOF_COMMENT

/*
 *    precedence table
 */
%nonassoc tLOWEST
%nonassoc tLBRACE_ARG

%nonassoc  kIF_MOD kUNLESS_MOD kWHILE_MOD kUNTIL_MOD 
%left  kOR kAND
%right kNOT
%nonassoc kDEFINED
%right '=' tOP_ASGN
%left kRESCUE_MOD
%right '?' ':'
%nonassoc tDOT2 tDOT3
%left  tOROP
%left  tANDOP
%nonassoc  tCMP tEQ tEQQ tNEQ tMATCH tNMATCH
%left  tGT tGEQ tLT tLEQ
%left  tPIPE tCARET
%left  tAMPER2
%left  tLSHFT tRSHFT
%left  tPLUS tMINUS
%left  tSTAR2 tDIVIDE tPERCENT
%right tUMINUS_NUM tUMINUS
%right tPOW
%right tBANG tTILDE tUPLUS

%token <Integer> tLAST_TOKEN

%%
program     : {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();

              } compstmt opt_eof_comment{
                  if ($2 != null) {
                      /* last expression should not be void */
                      if ($2 instanceof BlockNode) {
                          support.checkUselessStatement($<BlockNode>2.getLast());
                      } else {
                          support.checkUselessStatement($2);
                      }
                  }
                  Node node = support.introduceComment($2, new Object[]{$<>3});
                  support.getResult().setAST(support.appendToBlock(support.getResult().getAST(), node));
                  support.updateTopLocalVariables();
              }

bodystmt    : compstmt
              opt_rescue
              opt_else
              opt_ensure {
                 Node node = $1;

		 if ($2 != null) {
		   node = new RescueNode(getPosition($<ISourcePositionHolder>1, true), $1, $2, $3);
		 } else if ($3 != null) {
		       warnings.warn(getPosition($<ISourcePositionHolder>1), "else without rescue is useless");
                       node = support.appendToBlock($1, $3);
		 }
		 if ($4 != null) {
		    node = new EnsureNode(getPosition($<ISourcePositionHolder>1), node, $4);
		 }

		 $$ = node;
             }

compstmt    : stmts opt_terms {
                  if ($1 instanceof BlockNode) {
                     support.checkUselessStatements($<BlockNode>1);
		  }
                  $$ = $1;
              }

stmts         : none
              | stmt {
                    $$ = support.newline_node($1, getPosition($<ISourcePositionHolder>1, true));
                }
              | stmts terms stmt {
		    			Node node = support.introduceComment($3, new Object[]{$<>2});
		    			$$ = support.appendToBlock($1, support.newline_node(node, getPosition($<ISourcePositionHolder>1, true)));
                }
              | error stmt {
                    $$ = $2;
                }

stmt          : kALIAS fitem {
                    lexer.setState(LexState.EXPR_FNAME);
                } fitem {
                   AliasNode node = new AliasNode(getPosition($<ISourcePositionHolder>1), (String) $2.getValue(), (String) $4.getValue());
                    $$ = support.introduceComment(node, new Object[]{$1, $2, $4});
                }
              | kALIAS tGVAR tGVAR {
              		VAliasNode node = new VAliasNode(getPosition($<ISourcePositionHolder>1), (String) $2.getValue(), (String) $3.getValue());
                    $$ = support.introduceComment(node, new Object[]{$1, $2, $3});
                }
              | kALIAS tGVAR tBACK_REF {
                    VAliasNode node = new VAliasNode(getPosition($<ISourcePositionHolder>1), (String) $2.getValue(), "$" + ((BackRefNode)$3.getValue()).getType()); // XXX
                    $$ = support.introduceComment(node, new Object[]{$1, $2, $3});
                }
              | kALIAS tGVAR tNTH_REF {
                    yyerror("can't make alias for the number variables");
                    $$ = null; //XXX 0
                }
              
              | kUNDEF undef_list {
                    $$ = support.introduceComment($2, new Object[]{$1});
                }
              | stmt kIF_MOD expr_value {
                    IfNode node = new IfNode(support.union($<ISourcePositionHolder>1.getPosition(), $<Node>3.getPosition()), support.getConditionNode($3), $1, null);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | stmt kUNLESS_MOD expr_value {
                    IfNode node = new IfNode(getPosition($<ISourcePositionHolder>1), support.getConditionNode($3), null, $1);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | stmt kWHILE_MOD expr_value {
                    WhileNode node;
                    if ($1 != null && $1 instanceof BeginNode) {
                        node = new WhileNode(getPosition($<ISourcePositionHolder>1), support.getConditionNode($3), $<BeginNode>1.getBodyNode(), false);
                    } else {
                        node = new WhileNode(getPosition($<ISourcePositionHolder>1), support.getConditionNode($3), $1, true);
                    }
                    
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | stmt kUNTIL_MOD expr_value {
                    UntilNode node;
                    if ($1 != null && $1 instanceof BeginNode) {
                    		node = new UntilNode(getPosition($<ISourcePositionHolder>1), support.getConditionNode($3), $<BeginNode>1.getBodyNode());
                    } else {
                        node = new UntilNode(getPosition($<ISourcePositionHolder>1), support.getConditionNode($3), $1);
                    }
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | stmt kRESCUE_MOD stmt
                {
		  			RescueNode node = new RescueNode(getPosition($<ISourcePositionHolder>1), $1, new RescueBodyNode(getPosition($<ISourcePositionHolder>1), null,$3, null), null);
		  			$$ = support.introduceComment(node, new Object[]{$2});
                }
              | klBEGIN
                {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("BEGIN in method");
                    }
                    support.getLocalNames().push(new LocalNamesElement());
                } tLCURLY compstmt '}' {
                
                		Node node = support.introduceComment($4, new Object[]{$1, $3, $<>5});
                    support.getResult().addBeginNode(new ScopeNode(getPosition($1, true), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), node));
                    support.getLocalNames().pop();
                    $$ = null; //XXX 0;
                }
              | klEND tLCURLY compstmt '}' {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("END in method; use at_exit");
                    }
                    Node node = support.introduceComment($3, new Object[]{$1,$2,$<>4});
                    support.getResult().addEndNode(new IterNode(getPosition($<ISourcePositionHolder>1), null, new PostExeNode(getPosition($<ISourcePositionHolder>1)), node));
                    $$ = null;
                }
              | lhs '=' command_call {
                    support.checkExpression($3);
                    Node node = support.node_assign($1, $3);
                    $$ = support.introduceComment(node, new Object[]{$<>2});
                }
              | mlhs '=' command_call {
                    support.checkExpression($3);
		    if ($1.getHeadNode() != null) {
		        $1.setValueNode(new ToAryNode(getPosition($<ISourcePositionHolder>1), $3));
		    } else {
		        $1.setValueNode(new ArrayNode(getPosition($<ISourcePositionHolder>1)).add($3));
		    }
		    $$ = support.introduceComment($1, new Object[]{$<>2});
                }
              
              | var_lhs tOP_ASGN command_call {
 		    support.checkExpression($3);
 		    
 		    Object[] tokens = new Object[]{$2};
		    if ($1 != null) {
		        String name = $<INameNode>1.getName();
			String asgnOp = (String) $2.getValue();
		        if (asgnOp.equals("||")) {
	                    $<AssignableNode>1.setValueNode($3);
	                    OpAsgnOrNode node = new OpAsgnOrNode(getPosition($<ISourcePositionHolder>1), support.gettable(name, $<ISourcePositionHolder>1.getPosition()), $<Node>1);
	                    $$ = support.introduceComment(node, tokens);
			    /* XXX
			    if (is_asgn_or_id(vid)) {
				$$->nd_aid = vid;
			    }
			    */
			} else if (asgnOp.equals("&&")) {
	                    $<AssignableNode>1.setValueNode($3);
                            OpAsgnAndNode node = new OpAsgnAndNode(getPosition($<ISourcePositionHolder>1), support.gettable(name, $<ISourcePositionHolder>1.getPosition()), $<Node>1);
                            $$ = support.introduceComment(node, tokens);
			} else {

                            if ($1 != null) {
                                $<AssignableNode>1.setValueNode(support.getOperatorCallNode(support.gettable(name, $<ISourcePositionHolder>1.getPosition()), asgnOp, $3));
                            		$$ = support.introduceComment($1, tokens);
                            }
                            else{
			    $$ = $1;
                            }

			}
		    } else {
 		        $$ = null;
		    }
		}
              | primary_value '[' aref_args ']' tOP_ASGN command_call {
                    /* Much smaller than ruby block */
                    OpElementAsgnNode node = new OpElementAsgnNode(getPosition($<ISourcePositionHolder>1), $1, (String) $5.getValue(), $3, $6);
                    $$ = support.introduceComment(node, new Object[]{$<>2,$<>4,$5});

                }
              | primary_value tDOT tIDENTIFIER tOP_ASGN command_call {
                    OpAsgnNode node = new OpAsgnNode(getPosition($<ISourcePositionHolder>1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
                    $$ = support.introduceComment(node, new Object[]{$2,$3,$4});
                }
              | primary_value tDOT tCONSTANT tOP_ASGN command_call {
                    OpAsgnNode node = new OpAsgnNode(getPosition($<ISourcePositionHolder>1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
                    $$ = support.introduceComment(node, new Object[]{$2,$3,$4});
                }
              | primary_value tCOLON2 tIDENTIFIER tOP_ASGN command_call {
  					OpAsgnNode node = new OpAsgnNode(getPosition($<ISourcePositionHolder>1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
  					$$ = support.introduceComment(node, new Object[]{$2,$3,$4});
                }
              | backref tOP_ASGN command_call {
                    support.backrefAssignError($1);
                    $$ = null;
                }
              | lhs '=' mrhs {
                    $$ = support.node_assign($1, new SValueNode(getPosition($<ISourcePositionHolder>1), $3));
                }
 	      | mlhs '=' arg_value {
                    if ($1.getHeadNode() != null) {
		        $1.setValueNode(new ToAryNode(getPosition($<ISourcePositionHolder>1), $3));
		    } else {
		        $1.setValueNode(new ArrayNode(getPosition($<ISourcePositionHolder>1)).add($3));
		    }
		    $$ = support.introduceComment($1, new Object[]{$<>2});
		}
	      | mlhs '=' mrhs {
                    $<AssignableNode>1.setValueNode($3);
		    $$ = support.introduceComment($1, new Object[]{$<>2});
		}
              | expr 

expr          : command_call 
              | expr kAND expr {
                    Node node = support.newAndNode($<Token>2.getPosition(), $1, $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | expr kOR expr {
                    Node node = support.newOrNode($<Token>2.getPosition(), $1, $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | kNOT expr {
                    NotNode node = new NotNode($<Token>1.getPosition(), support.getConditionNode($2));
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | tBANG command_call {
                    NotNode node = new NotNode(getPosition($<ISourcePositionHolder>1), support.getConditionNode($2));
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | arg

expr_value    : expr {
                    support.checkExpression($1);
		    $$ = $1; //Do we really need this set? $1 is $$?
		}

command_call  : command
              | block_command
              | kRETURN call_args {
                    ReturnNode node = new ReturnNode(getPosition($<ISourcePositionHolder>1), support.ret_args($2, getPosition($<ISourcePositionHolder>1)));
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | kBREAK call_args {
                    BreakNode node = new BreakNode(getPosition($<ISourcePositionHolder>1), support.ret_args($2, getPosition($<ISourcePositionHolder>1)));
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | kNEXT call_args {
                    NextNode node = new NextNode(getPosition($<ISourcePositionHolder>1), support.ret_args($2, getPosition($<ISourcePositionHolder>1)));
                    $$ = support.introduceComment(node, new Object[]{$1});
                }

block_command : block_call
              | block_call tDOT operation2 command_args {
                    Node node = support.new_call($1, $3, $4);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | block_call tCOLON2 operation2 command_args {
                    Node node = support.new_call($1, $3, $4);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }

cmd_brace_block	: tLBRACE_ARG {
                      support.getBlockNames().push(new BlockNamesElement());
		  } opt_block_var compstmt '}' {
                      IterNode node = new IterNode(getPosition($<ISourcePositionHolder>1), $3, $4, null);
                      $$ = support.introduceComment(node, new Object[]{$1, $<>5});
                      support.getBlockNames().pop();
		  }

command       : operation command_args  %prec tLOWEST {
                    $$ = support.new_fcall((String) $1.getValue(), $2, $1); // .setPosFrom($2);
                }
 	      | operation command_args cmd_brace_block {
                    $$ = support.new_fcall((String) $1.getValue(), $2, $1); 
	            if ($3 != null) {
                        if ($$ instanceof BlockPassNode) {
                            throw new SyntaxException(getPosition($<ISourcePositionHolder>1), "Both block arg and actual block given.");
                        }
                        $3.setIterNode($<Node>$);
                        $$ = $2;
		   }
                }
	      | primary_value tDOT operation2 command_args %prec tLOWEST {
                    Node node = support.new_call($1, $3, $4); //.setPosFrom($1);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
 	      | primary_value tDOT operation2 command_args cmd_brace_block {
                    $$ = support.new_call($1, $3, $4);
		    if ($5 != null) {
		        if ($$ instanceof BlockPassNode) {
                            throw new SyntaxException(getPosition($<ISourcePositionHolder>1), "Both block arg and actual block given.");
                        }
                        $5.setIterNode($<Node>$);
			$$ = $5;
		    }
		 }
              | primary_value tCOLON2 operation2 command_args %prec tLOWEST {
                    Node node = support.new_call($1, $3, $4);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
 	      | primary_value tCOLON2 operation2 command_args cmd_brace_block {
                    $$ = support.new_call($1, $3, $4);
		    if ($5 != null) {
		        if ($$ instanceof BlockPassNode) {
                            throw new SyntaxException(getPosition($<ISourcePositionHolder>1), "Both block arg and actual block given.");
                        }
                        $5.setIterNode($<Node>$);
			$$ = $5;
		    }
	        }
              | kSUPER command_args {
				    Node node = support.new_super($2, $1); // .setPosFrom($2);
				    $$ = support.introduceComment(node, new Object[]{$1});
		}
              | kYIELD command_args {
                    Node node = support.new_yield(getPosition($<ISourcePositionHolder>1), $2);
                    $$ = support.introduceComment(node, new Object[]{$1});
		}

mlhs          : mlhs_basic
              | tLPAREN mlhs_entry ')' {
                    MultipleAsgnNode mlhs = new MultipleAsgnNode($2.getPosition(), $2.getHeadNode(), support.introduceComment($2.getArgsNode(), new Object[]{$1, $<>3}));
                    $$ = mlhs;
		}

mlhs_entry    : mlhs_basic
              | tLPAREN mlhs_entry ')' {
	            $$ = new MultipleAsgnNode(getPosition($<ISourcePositionHolder>1), new ArrayNode(getPosition($<ISourcePositionHolder>1)).add($2), null);
                }

mlhs_basic    : mlhs_head {
                    $$ = new MultipleAsgnNode(getPosition($<ISourcePositionHolder>1), $1, null);
                }
              | mlhs_head mlhs_item {
                    $$ = new MultipleAsgnNode(support.union($<Node>1, $<Node>2), $1.add($2), null);
                    $<Node>1.setPosition(support.union($<Node>1, $<Node>2));
                
                }
              | mlhs_head tSTAR mlhs_node {
                    $$ = new MultipleAsgnNode(getPosition($<ISourcePositionHolder>1), $1, support.introduceComment($3, new Object[]{$2}));
                }
              | mlhs_head tSTAR {
                    $$ = new MultipleAsgnNode(getPosition($<ISourcePositionHolder>1), $1, support.introduceComment(new StarNode(getPosition(null)), new Object[]{$2}));
                }
              | tSTAR mlhs_node {
                    $$ = new MultipleAsgnNode(getPosition($<ISourcePositionHolder>1), null, support.introduceComment($2, new Object[]{$1}));
                }
              | tSTAR {
                    $$ = new MultipleAsgnNode(getPosition($<ISourcePositionHolder>1), null, support.introduceComment(new StarNode(getPosition(null)), new Object[]{$1}));
                }

mlhs_item     : mlhs_node
              | tLPAREN mlhs_entry ')' {
                    $$ = support.introduceComment($2, new Object[]{$1, $<>3});
                }

mlhs_head     : mlhs_item ',' {
                    
                    $$ = new ArrayNode($<Node>1.getPosition()).add(support.introduceComment($1, new Object[]{$<>2}));
                }
              | mlhs_head mlhs_item ',' {
                    $$ = $1.add(support.introduceComment($2, new Object[]{$<>3}));
                }

mlhs_node     : variable {
                    Node node = support.assignable($<ISourcePositionHolder>1.getPosition(), $1, null);
                    $$ = support.introduceComment(node, new Object[]{$1});
                
                }
              | primary_value '[' aref_args ']' {
                    Node node = support.getElementAssignmentNode($1, $3);
                    $$ = support.introduceComment(node, new Object[]{$<>2, $<>4});
                }
              | primary_value tDOT tIDENTIFIER {
                    Node node = support.getAttributeAssignmentNode($1, (String) $3.getValue());
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
                }
              | primary_value tCOLON2 tIDENTIFIER {
                    Node node = support.getAttributeAssignmentNode($1, (String) $3.getValue());
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
                }
              | primary_value tDOT tCONSTANT {
                    Node node = support.getAttributeAssignmentNode($1, (String) $3.getValue());
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
                }
 	      | primary_value tCOLON2 tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
			
                    Node node = new ConstDeclNode(support.union($1, $3), $1, (String) $3.getValue(), null);
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
		}
 	      | tCOLON3 tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }

		    /* ERROR:  VEry likely a big error. */
                    Node node = new Colon3Node(support.union($1, $2), (String) $2.getValue());
                    $$ = support.introduceComment(node, new Object[]{$1,$2});
		    /* ruby $$ = NEW_CDECL(0, 0, NEW_COLON3($2)); */
		    }

              | backref {
	            support.backrefAssignError($1);
                    $$ = null;
                }

lhs           : variable {
                    Node node = support.assignable($<ISourcePositionHolder>1.getPosition(), $1, null);
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | primary_value '[' aref_args ']' {
                    Node node = support.getElementAssignmentNode($1, $3);
                    $$ = support.introduceComment(node, new Object[]{$<>2,$<>4});
                }
              | primary_value tDOT tIDENTIFIER {
                    Node node = support.getAttributeAssignmentNode($1, (String) $3.getValue());
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
                }
              | primary_value tCOLON2 tIDENTIFIER {
                    Node node = support.getAttributeAssignmentNode($1, (String) $3.getValue());
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
 	        }
              | primary_value tDOT tCONSTANT {
                    Node node = support.getAttributeAssignmentNode($1, (String) $3.getValue());
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
                }
   	      | primary_value tCOLON2 tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
                    Node node = new ConstDeclNode(support.union($1, $3), $1, (String) $3.getValue(), null);
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
	        }
	      | tCOLON3 tCONSTANT {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }

		    /* ERROR:  VEry likely a big error. */
                    Colon3Node node = new Colon3Node(support.union($1, $2), (String) $2.getValue());
                    $$ = support.introduceComment(node, new Object[]{$1,$2});
		    /* ruby $$ = NEW_CDECL(0, 0, NEW_COLON3($2)); */
	        }
              | backref {
                    support.backrefAssignError($1);
                    $$ = null;
		}

cname         : tIDENTIFIER {
                    yyerror("class/module name must be CONSTANT");
                }
              | tCONSTANT

cpath	      : tCOLON3 cname {
                    Colon3Node node = new Colon3Node(support.union($1, $2), (String) $2.getValue());;
                    Object[] tokens = new Object[]{$1,$2};
                    $$ = support.introduceComment(node, tokens); 
		}
	      | cname {
                    // $1 was $$ in ruby?
                    Colon2Node node = new Colon2Node($<ISourcePositionHolder>1.getPosition(), null, (String) $1.getValue());
                    Object[] tokens = new Object[]{$1};
                    $$ = support.introduceComment(node, tokens);
                    
 	        }
	      | primary_value tCOLON2 cname {
                    Object[] tokens = new Object[]{$2};
                    Colon2Node node = new Colon2Node(support.union($1, $3), support.introduceComment($1, tokens), (String) $3.getValue());
                    
                    tokens = new Object[]{$3};
                    $$ = support.introduceComment(node, tokens);
		}

fname         : tIDENTIFIER
              | tCONSTANT
              | tFID
              | op {
                    lexer.setState(LexState.EXPR_END);
                    $$ = $1;
                }
              | reswords {
                    lexer.setState(LexState.EXPR_END);
                    $$ = $<>1;
                }

fitem         : fname
              | symbol

undef_list    : fitem {
                    UndefNode node = new UndefNode(getPosition($<ISourcePositionHolder>1), (String) $1.getValue());
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | undef_list ',' {
                    lexer.setState(LexState.EXPR_FNAME);
	        } fitem {
	        			Node node = support.appendToBlock($1, new UndefNode(getPosition($<ISourcePositionHolder>1), (String) $4.getValue()));
                    $$ = support.introduceComment(node, new Object[]{$<>2,$4});
                }

 op            : tPIPE  { $1.setValue("|"); $$ = $1; }
              | tCARET  { $1.setValue("^"); $$ = $1; }
              | tAMPER2 { $1.setValue("&"); $$ = $1; }
              | tCMP    { $1.setValue("<=>"); $$ = $1; }
              | tEQ     { $1.setValue("=="); $$ = $1; }
              | tEQQ    { $1.setValue("==="); $$ = $1;}
              | tMATCH  { $1.setValue("=~"); $$ = $1; }
              | tGT     { $1.setValue(">"); $$ = $1; }
              | tGEQ    { $1.setValue(">="); $$ = $1; }
              | tLT     { $1.setValue("<"); $$ = $1; }
              | tLEQ    { $1.setValue("<="); $$ = $1; }
              | tLSHFT  { $1.setValue("<<"); $$ = $1; }
              | tRSHFT  { $1.setValue(">>"); $$ = $1; }
              | tPLUS   { $1.setValue("+"); $$ = $1; }
              | tMINUS  { $1.setValue("-"); $$ = $1; }
              | tSTAR2  { $1.setValue("*"); $$ = $1; }
              | tSTAR   { $1.setValue("*"); $$ = $1; }
              | tDIVIDE { $1.setValue("/"); $$ = $1; }
              | tPERCENT { $1.setValue("%"); $$ = $1; }
              | tPOW    { $1.setValue("**"); $$ = $1; }
              | tTILDE  { $1.setValue("~"); $$ = $1; }
              | tUPLUS  { $1.setValue("+@"); $$ = $1; }
              | tUMINUS { $1.setValue("-@"); $$ = $1; }
              | tAREF   { $1.setValue("[]"); $$ = $1; }
              | tASET   { $1.setValue("[]="); $$ = $1; }
              | tBACK_REF2 {  $$ = $1; }


reswords	: k__LINE__ | k__FILE__  | klBEGIN | klEND
		| kALIAS | kAND | kBEGIN | kBREAK | kCASE | kCLASS | kDEF
		| kDEFINED | kDO | kELSE | kELSIF | kEND | kENSURE | kFALSE
		| kFOR | kIN | kMODULE | kNEXT | kNIL | kNOT
		| kOR | kREDO | kRESCUE | kRETRY | kRETURN | kSELF | kSUPER
		| kTHEN | kTRUE | kUNDEF | kWHEN | kYIELD
		| kIF_MOD | kUNLESS_MOD | kWHILE_MOD | kUNTIL_MOD | kRESCUE_MOD

arg           : lhs '=' arg {
					Node node = support.node_assign($1, $3);
                    $$ = support.introduceComment(node, new Object[]{$<>2});
		    $<Node>$.setPosition(support.union($1, $3));
                }
	      | lhs '=' arg kRESCUE_MOD arg {
	      			RescueNode rescNode = new RescueNode(getPosition($<ISourcePositionHolder>1), $3, new RescueBodyNode(getPosition($<ISourcePositionHolder>1), null ,$5, null), null);
	      			Node node = support.node_assign($1, support.introduceComment(rescNode, new Object[]{$4}));
                    $$ = support.introduceComment(node, new Object[]{$<>2});
		}
	      | var_lhs tOP_ASGN arg {
		    support.checkExpression($3);
		    Object[] tokens = new Object[]{$1,$2};
		    if ($1 != null) {
		        String name = $<INameNode>1.getName();
			String asgnOp = (String) $2.getValue();

		        if (asgnOp.equals("||")) {
	                    $<AssignableNode>1.setValueNode($3);
	                    OpAsgnOrNode node = new OpAsgnOrNode(getPosition($<ISourcePositionHolder>1), support.gettable(name, $<ISourcePositionHolder>1.getPosition()), $1);
	                    $$ = support.introduceComment(node, tokens);
			    /* FIXME
			    if (is_asgn_or_id(vid)) {
				$$->nd_aid = vid;
			    }
			    */
			} else if (asgnOp.equals("&&")) {
	                    $<AssignableNode>1.setValueNode($3);
	                    OpAsgnAndNode node = new OpAsgnAndNode(getPosition($<ISourcePositionHolder>1), support.gettable(name, $<ISourcePositionHolder>1.getPosition()), $<Node>1);
                            $$ = support.introduceComment(node, tokens);
			} else {
			    
                            if ($1 != null) {
			      				$<AssignableNode>1.setValueNode(support.getOperatorCallNode(support.gettable(name, $<ISourcePositionHolder>1.getPosition()), asgnOp, $3));
                            		$$ = support.introduceComment($1, tokens);
                            }else{
			    $$ = $1;
                            }
			}
		    } else {
 		        $$ = null; /* XXX 0; */
		    }
                }
              | primary_value '[' aref_args ']' tOP_ASGN arg {
                    OpElementAsgnNode node = new OpElementAsgnNode(getPosition($<ISourcePositionHolder>1), $1, (String) $5.getValue(), $3, $6);
                    $$ = support.introduceComment(node, new Object[]{$<>2, $<>4, $<>5});
                }
              | primary_value tDOT tIDENTIFIER tOP_ASGN arg {
              		OpAsgnNode node = new OpAsgnNode(getPosition($<ISourcePositionHolder>1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
                    $$ = support.introduceComment(node, new Object[]{$2, $3, $4});
                }
              | primary_value tDOT tCONSTANT tOP_ASGN arg {
                    OpAsgnNode node = new OpAsgnNode(getPosition($<ISourcePositionHolder>1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
                    $$ = support.introduceComment(node, new Object[]{$2, $3, $4});
                }
              | primary_value tCOLON2 tIDENTIFIER tOP_ASGN arg {
                    OpAsgnNode node = new OpAsgnNode(getPosition($<ISourcePositionHolder>1), $1, $5, (String) $3.getValue(), (String) $4.getValue());
                    $$ = support.introduceComment(node, new Object[]{$2, $3, $4});
                }
	      | primary_value tCOLON2 tCONSTANT tOP_ASGN arg {
		    yyerror("constant re-assignment");
		    $$ = null;
	        }
	      | tCOLON3 tCONSTANT tOP_ASGN arg {
		    yyerror("constant re-assignment");
		    $$ = null;
	        }
              | backref tOP_ASGN arg {
                    support.backrefAssignError($1);
                    $$ = null;
                }
              | arg tDOT2 arg {
		    support.checkExpression($1);
		    support.checkExpression($3);
                    DotNode node = new DotNode(getPosition($<ISourcePositionHolder>1), $1, $3, false);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tDOT3 arg {
		    support.checkExpression($1);
		    support.checkExpression($3);
                 	DotNode node = new DotNode(getPosition($<ISourcePositionHolder>1), $1, $3, true);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tPLUS arg {
              		Node node = support.getOperatorCallNode($1, "+", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tMINUS arg {
              		Node node = support.getOperatorCallNode($1, "-", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tSTAR2 arg {
              		Node node = support.getOperatorCallNode($1, "*", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tDIVIDE arg {
              		Node node = support.getOperatorCallNode($1, "/", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tPERCENT arg {
              		Node node = support.getOperatorCallNode($1, "%", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tPOW arg {
		      		Node node = support.getOperatorCallNode($1, "**", $3);
		      		$$ = support.introduceComment(node, new Object[]{$2});
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
	      | tUMINUS_NUM tINTEGER tPOW arg {
                    Object number = $2.getValue();
					Node node = support.getOperatorCallNode(support.getOperatorCallNode((number instanceof Long ? (Node) new FixnumNode(getPosition($<ISourcePositionHolder>1), ((Long) number).longValue()) : (Node)new BignumNode(getPosition($<ISourcePositionHolder>1), ((BigInteger) number))), "**", $4), "-@");
                    $$ = support.introduceComment(node, new Object[]{$1, $2, $3});
                }
	      | tUMINUS_NUM tFLOAT tPOW arg {
  // ENEBO: Seems like this should be $2
  // TCORBAT: Done. Should work so far.
  //$$ = support.getOperatorCallNode(support.getOperatorCallNode(new FloatNode(getPosition($<ISourcePositionHolder>1), ((Double) $1.getValue()).doubleValue()), "**", $4), "-@");
                                
                Double number = (Double)$2.getValue();
                Node node = support.getOperatorCallNode(support.getOperatorCallNode(new FloatNode(getPosition($1), number.doubleValue()), "**", $4), "-@");
                $$ = support.introduceComment(node, new Object[]{$1, $2, $3});
                
                }
              | tUPLUS arg {
              		Node node;
 	            if ($2 != null && $2 instanceof ILiteralNode) {
			        		node = $2;
			    		 }else {
	                    node = support.getOperatorCallNode($2, "+@");
		    }
			    		$$ = support.introduceComment(node, new Object[]{$1});
                }
	      | tUMINUS arg {
                    Node node = support.getOperatorCallNode($2, "-@");
                    $$ = support.introduceComment(node, new Object[]{$1});
		}
              | arg tPIPE arg {
              		Node node = support.getOperatorCallNode($1, "|", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tCARET arg {
              		Node node = support.getOperatorCallNode($1, "^", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tAMPER2 arg {
              		Node node = support.getOperatorCallNode($1, "&", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tCMP arg {
              		Node node = support.getOperatorCallNode($1, "<=>", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tGT arg {
              		Node node = support.getOperatorCallNode($1, ">", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tGEQ arg {
              		Node node = support.getOperatorCallNode($1, ">=", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tLT arg {
              		Node node = support.getOperatorCallNode($1, "<", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tLEQ arg {
              		Node node = support.getOperatorCallNode($1, "<=", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tEQ arg {
              		Node node = support.getOperatorCallNode($1, "==", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tEQQ arg {
              		Node node = support.getOperatorCallNode($1, "===", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tNEQ arg {
                    Node node = new NotNode(getPosition($<ISourcePositionHolder>1), support.getOperatorCallNode($1, "==", $3));
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tMATCH arg {
                    Node node = support.getMatchNode($1, $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tNMATCH arg {
              		Node node = new NotNode(getPosition($<ISourcePositionHolder>1), support.getMatchNode($1, $3));
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | tBANG arg {
                    Node node = new NotNode(getPosition($<ISourcePositionHolder>1), support.getConditionNode($2));
                		$$ = support.introduceComment(node, new Object[]{$1});
                }
              | tTILDE arg {
                    Node node = support.getOperatorCallNode($2, "~");
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | arg tLSHFT arg {
              		Node node = support.getOperatorCallNode($1, "<<", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tRSHFT arg {
              		Node node = support.getOperatorCallNode($1, ">>", $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tANDOP arg {
                    Node node = support.newAndNode($<Token>2.getPosition(), $1, $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | arg tOROP arg {
              		Node node = support.newOrNode($<Token>2.getPosition(), $1, $3);
                    $$ = support.introduceComment(node, new Object[]{$2});
                }
              | kDEFINED opt_nl {
	            support.setInDefined(true);
		} arg {
                    support.setInDefined(false);
                    DefinedNode node = new DefinedNode(getPosition($<ISourcePositionHolder>1), $4);
                    $$ = support.introduceComment(node, new Object[]{$1,$<>2});
                }
              | arg '?' arg ':' arg {
              		IfNode node = new IfNode(getPosition($<ISourcePositionHolder>1), support.getConditionNode($1), $3, $5);
                    $$ = support.introduceComment(node, new Object[]{$<>2, $<>4});
                }
              | primary {
                    $$ = $1;
                }

arg_value     : arg {
		    support.checkExpression($1);
	            $$ = $1;   
		}

aref_args     : none
              | command opt_nl {
                    warnings.warn(getPosition($<ISourcePositionHolder>1), "parenthesize argument(s) for future version");
                    $$ = new ArrayNode(getPosition($<ISourcePositionHolder>1)).add($1);
                }
              | args trailer {
                    $$ = support.commentLastElement($1, new Object[]{$<>2});
                }
              | args ',' tSTAR arg opt_nl {
              		support.commentLastElement($1, new Object[]{$<>2});
                    support.checkExpression($4);
                    Node node = support.introduceComment($4, new Object[]{$3});
                    $$ = support.arg_concat(getPosition($<ISourcePositionHolder>1), $1, node);
                }
              | assocs trailer {
              		Node node = support.introduceComment(new HashNode(getPosition(null), $1), new Object[]{$<>2});
                    $$ = new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(node);
                }
              | tSTAR arg opt_nl {
                    support.checkExpression($2);
                    Node node = support.introduceComment(new SplatNode(getPosition($<ISourcePositionHolder>1), $2), new Object[]{$1});
		    			$$ = new NewlineNode(getPosition($<ISourcePositionHolder>1), node);
                }

paren_args    : tLPAREN2 none_list ')' {
                    $$ = $2;
                }
              | tLPAREN2 call_args opt_nl ')' {
                    $$ = support.introduceComment($2, new Object[]{$1, $<>4});
                }
              | tLPAREN2 block_call opt_nl ')' {
                    warnings.warn(getPosition($<ISourcePositionHolder>1), "parenthesize argument(s) for future version");
                    
                    Node node = new ArrayNode(getPosition($<ISourcePositionHolder>1)).add($2);
                    $$ = support.introduceComment(node, new Object[]{$1,$<>4});
                }
              | tLPAREN2 args ',' block_call opt_nl ')' {
                    warnings.warn(getPosition($<ISourcePositionHolder>1), "parenthesize argument(s) for future version");
                    support.commentLastElement($2, new Object[]{$<>3});
                    $2.add($4);
                    $$ = support.introduceComment($2, new Object[]{$1,$<>6});
                }

opt_paren_args: none
              | paren_args 

call_args     : command {
                    warnings.warn(getPosition($<ISourcePositionHolder>1), "parenthesize argument(s) for future version");
                    $$ = new ArrayNode(getPosition($<ISourcePositionHolder>1)).add($1);
                }
              | args opt_block_arg {
                    $$ = support.arg_blk_pass($1, $2);
                }
              | args ',' tSTAR arg_value opt_block_arg {
                    support.commentLastElement($1, new Object[]{$<>2});
                    
                    Node node = support.introduceComment($4, new Object[]{$3});
                    
                    $$ = support.arg_concat(getPosition($<ISourcePositionHolder>1), $1, node);
                    $$ = support.arg_blk_pass((Node)$$, $5);
                }
              | assocs opt_block_arg {
                    $$ = new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(new HashNode(getPosition(null), $1));
                    $$ = support.arg_blk_pass((Node)$$, $2);
                }
              | assocs ',' tSTAR arg_value opt_block_arg {
                    support.commentLastElement($1, new Object[]{$<>2});
                    
                    Node node = support.introduceComment($4, new Object[]{$3});
                    
                    $$ = support.arg_concat(getPosition($<ISourcePositionHolder>1), new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(new HashNode(getPosition(null), $1)), node);
                    $$ = support.arg_blk_pass((Node)$$, $5);
                }
              | args ',' assocs opt_block_arg {
                    support.commentLastElement($1, new Object[]{$<>2});
                    
                    $$ = $1.add(new HashNode(getPosition(null), $3));
                    $$ = support.arg_blk_pass((Node)$$, $4);
                }
              | args ',' assocs ',' tSTAR arg opt_block_arg {
                    support.commentLastElement($1, new Object[]{$<>2});
                    support.commentLastElement($3, new Object[]{$<>4});
                    
                    support.checkExpression($6);
                    
                    Node node = support.introduceComment($6, new Object[]{$5});
                    
		    			$$ = support.arg_concat(getPosition($<ISourcePositionHolder>1), $1.add(new HashNode(getPosition(null), $3)), node);
                    $$ = support.arg_blk_pass((Node)$$, $7);
                }
              | tSTAR arg_value opt_block_arg {
                    Node node = support.introduceComment($2, new Object[]{$1});
                    $$ = support.arg_blk_pass(new SplatNode(getPosition($<ISourcePositionHolder>1), node), $3);
                }
              | block_arg {
	        }

call_args2	: arg_value ',' args opt_block_arg {
                      Node node = support.introduceComment($1, new Object[]{$<>2});
                      $$ = support.arg_blk_pass(support.list_concat(new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(node), $3), $4);
		  }
		| arg_value ',' block_arg {
                      Node node = support.introduceComment($1, new Object[]{$<>2});
                      $$ = support.arg_blk_pass(new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(node), $3);
                  }
		| arg_value ',' tSTAR arg_value opt_block_arg {
                      Node argNode = support.introduceComment($1, new Object[]{$<>2});
                      Node starNode = support.introduceComment($4, new Object[]{$3});
                      $$ = support.arg_concat(getPosition($<ISourcePositionHolder>1), new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(argNode), starNode);
                      $$ = support.arg_blk_pass((Node)$$, $5);
		  }
		| arg_value ',' args ',' tSTAR arg_value opt_block_arg {
                      Node argNode = support.introduceComment($1, new Object[]{$<>2});
                      Node starNode = support.introduceComment($6, new Object[]{$5});
                      support.commentLastElement($3, new Object[]{$<>4});
                      
                      $$ = support.arg_concat(getPosition($<ISourcePositionHolder>1), support.list_concat(new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(argNode), new HashNode(getPosition(null), $3)), starNode);
                      $$ = support.arg_blk_pass((Node)$$, $7);
		  }
		| assocs opt_block_arg {
                      $$ = new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(new HashNode(getPosition(null), $1));
                      $$ = support.arg_blk_pass((Node)$$, $2);
		  }
		| assocs ',' tSTAR arg_value opt_block_arg {
                      Node node = support.introduceComment($4, new Object[]{$3});
                      support.commentLastElement($1, new Object[]{$<>2});
                      
                      $$ = support.arg_concat(getPosition($<ISourcePositionHolder>1), new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(new HashNode(getPosition(null), $1)), node);
                      $$ = support.arg_blk_pass((Node)$$, $5);
		  }
		| arg_value ',' assocs opt_block_arg {
                      Node node = support.introduceComment($1, new Object[]{$<>2});
                      $$ = new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(node).add(new HashNode(getPosition(null), $3));
                      $$ = support.arg_blk_pass((Node)$$, $4);
		  }
		| arg_value ',' args ',' assocs opt_block_arg {
                      Node node = support.introduceComment($1, new Object[]{$<>2});
                      support.commentLastElement($3, new Object[]{$<>4});
                      
                      $$ = support.list_concat(new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(node), $3).add(new HashNode(getPosition(null), $5));
                      $$ = support.arg_blk_pass((Node)$$, $6);
		  }
		| arg_value ',' assocs ',' tSTAR arg_value opt_block_arg {
                      Node arg1Node = support.introduceComment($1, new Object[]{$<>2});
                      Node arg2Node = support.introduceComment($6, new Object[]{$5});
                      support.commentLastElement($3, new Object[]{$<>4});
                      
                      $$ = support.arg_concat(getPosition($<ISourcePositionHolder>1), new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(arg1Node).add(new HashNode(getPosition(null), $3)), arg2Node);
                      $$ = support.arg_blk_pass((Node)$$, $7);
		  }
		| arg_value ',' args ',' assocs ',' tSTAR arg_value opt_block_arg {
                      Node arg1Node = support.introduceComment($1, new Object[]{$<>2});
                      Node arg2Node = support.introduceComment($8, new Object[]{$7});
                      support.commentLastElement($3, new Object[]{$<>4});       
                      support.commentLastElement($5, new Object[]{$<>6});             
                      
                      $$ = support.arg_concat(getPosition($<ISourcePositionHolder>1), support.list_concat(new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(arg1Node), $3).add(new HashNode(getPosition(null), $5)), arg2Node);
                      $$ = support.arg_blk_pass((Node)$$, $9);
		  }
		| tSTAR arg_value opt_block_arg {
                      Node node = support.introduceComment($2, new Object[]{$1});
                      
                      $$ = support.arg_blk_pass(new SplatNode(getPosition($<ISourcePositionHolder>1), node), $3);
		  }
		| block_arg {}

command_args  : { 
		    $$ = new Long(lexer.getCmdArgumentState().begin());
		} open_args {
                    lexer.getCmdArgumentState().reset($<Long>1.longValue());
                    $$ = $2;
                }

open_args     : call_args
	        | tLPAREN_ARG  {                    
		    lexer.setState(LexState.EXPR_ENDARG);
		  } ')' {
                    warnings.warn(getPosition($<ISourcePositionHolder>1), "don't put space before argument parentheses");
		    $$ = null;
		  }
		| tLPAREN_ARG call_args2 {
		    lexer.setState(LexState.EXPR_ENDARG);
		  } ')' {
                    warnings.warn(getPosition($<ISourcePositionHolder>1), "don't put space before argument parentheses");
		    $$ = support.introduceComment($2, new Object[]{$1, $<>4});
		  }

block_arg     : tAMPER arg_value {
                    support.checkExpression($2);
                    Node node = new BlockPassNode(getPosition($<ISourcePositionHolder>1), $2);
                    $$ = support.introduceComment(node, new Object[]{$1});
                }

opt_block_arg : ',' block_arg {
					Node bodyNode = support.introduceComment($2.getBodyNode(), new Object[]{$<>1});
                    BlockPassNode node = new BlockPassNode($2.getPosition(), bodyNode);
                    node.setIterNode($2.getIterNode());
                    node.setArgsNode($2.getArgsNode());
                    $$ = node;
                }
              | none_block_pass

args          : arg_value {
				$$ = new ArrayNode($<Node>1.getPosition()).add($1);
                }
              | args ',' arg_value {
              		support.commentLastElement($1, new Object[]{$<>2});
                    $$ = $1.add($3);
                }

mrhs          : args ',' arg_value {
		    			support.commentLastElement($1, new Object[]{$<>2});
		    $$ = $1.add($3);
                }
 	      | args ',' tSTAR arg_value {
                    support.commentLastElement($1, new Object[]{$<>2});
                    Node node = support.introduceComment($4, new Object[]{$3});
                    $$ = support.arg_concat(getPosition($<ISourcePositionHolder>1), $1, node);
		}
              | tSTAR arg_value {  
              		Node node = support.introduceComment($2, new Object[]{$1}); 
                    $$ = new SplatNode(getPosition($<ISourcePositionHolder>1), node);
		}

primary       : literal
              | strings
              | xstring 
              | regexp
              | words
              | qwords
	      | var_ref
	      | backref
	      | tFID {
                    VCallNode node = new VCallNode($<ISourcePositionHolder>1.getPosition(), (String) $1.getValue());
                    $$ = support.introduceComment(node, new Object[]{$1});
		}
              | kBEGIN bodystmt
		kEND {
					BeginNode node = new BeginNode(support.union($1, $3), $2);
                    $$ = support.introduceComment(node, new Object[]{$1, $2});
		}
              | tLPAREN_ARG expr { lexer.setState(LexState.EXPR_ENDARG); } opt_nl ')' {
		    warnings.warn(getPosition($<ISourcePositionHolder>1), "(...) interpreted as grouped expression");
                    $$ = support.introduceComment($2, new Object[]{$1,$<>3, $<>4});
		}
              | tLPAREN compstmt ')' {
	            		$$ = support.introduceComment($2, new Object[]{$1, $<>3});
                }
              | primary_value tCOLON2 tCONSTANT {
                    Colon2Node node = new Colon2Node(support.union($1, $3), $1, (String) $3.getValue());
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
                }
              | tCOLON3 tCONSTANT {
                    Colon3Node node = new Colon3Node(support.union($1, $2), (String) $2.getValue());
                    $$ = support.introduceComment(node, new Object[]{$1, $2});
                }
              | primary_value '[' aref_args ']' {
                    CallNode node = new CallNode(getPosition($<ISourcePositionHolder>1), $1, "[]", $3);
                    $$ = support.introduceComment(node, new Object[]{$<>2,$<>4});
                }
              | tLBRACK aref_args ']' {
                    if ($2 == null) {
                        $$=  new ZArrayNode($<Token>1.getPosition()); /* zero length array*/

                    } else {
                        $$ = $2;
                    }
                    $$ = support.introduceComment($<Node>$, new Object[]{$1, $<>3});
                }
              | tLBRACE assoc_list '}' {
                   	HashNode node = new HashNode(getPosition($<ISourcePositionHolder>1), $2);
                    $$ = support.introduceComment(node, new Object[]{$1, $<>3});
                }
              | kRETURN {
		    			ReturnNode node = new ReturnNode($<Token>1.getPosition(), null);
		    			$$ = support.introduceComment(node, new Object[]{$1});
                }
              | kYIELD tLPAREN2 call_args ')' {
                    Node node = support.new_yield(getPosition($<ISourcePositionHolder>1), $3);
                    $$ = support.introduceComment(node, new Object[]{$1, $2, $<>4});
                }
              | kYIELD tLPAREN2 ')' {
                    YieldNode node = new YieldNode(getPosition($<ISourcePositionHolder>1), null, false);
                    $$ = support.introduceComment(node, new Object[]{$1, $2, $<>3});
                }
              | kYIELD {
                    YieldNode node = new YieldNode(getPosition($<ISourcePositionHolder>1), null, false);
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | kDEFINED opt_nl tLPAREN2 {
	            support.setInDefined(true);
		} expr ')' {
                    support.setInDefined(false);
                   	DefinedNode node = new DefinedNode(getPosition($<ISourcePositionHolder>1), $5); 
                    $$ = support.introduceComment(node, new Object[]{$1, $<>2, $3, $<>6});
                }
              | operation brace_block {
                    $2.setIterNode(new FCallNode(getPosition($<ISourcePositionHolder>1, true), (String) $1.getValue(), null));
                    $$ = support.introduceComment($2, new Object[]{$1});
                }
              | method_call
              | method_call brace_block {
		    if ($1 != null && $1 instanceof BlockPassNode) {
                        throw new SyntaxException(getPosition($<ISourcePositionHolder>1), "Both block arg and actual block given.");
		    }
                    $2.setIterNode($1);
                    $$ = $2;
                }
              | kIF expr_value then compstmt if_tail kEND {
              	IfNode node = new IfNode(support.union($<ISourcePositionHolder>1.getPosition(), getPosition($<ISourcePositionHolder>1)), support.getConditionNode($2), $4, $5);
              	$$ = support.introduceComment(node, new Object[]{$1, $<>3, $5, $6});
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
		            NODE *tmp = $$->nd_body;
		            $$->nd_body = $$->nd_else;
		            $$->nd_else = tmp;
			    } */
                }
              | kUNLESS expr_value then compstmt opt_else kEND {
                    IfNode node = new IfNode(getPosition($<ISourcePositionHolder>1), support.getConditionNode($2), $5, $4);
                    $$ = support.introduceComment(node, new Object[]{$1, $<>3, $5, $6});
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
		            NODE *tmp = $$->nd_body;
		            $$->nd_body = $$->nd_else;
		            $$->nd_else = tmp;
			    } */
                }
              | kWHILE { 
	            lexer.getConditionState().begin();
		} expr_value do {
		    lexer.getConditionState().end();
		} compstmt kEND {
                   	WhileNode node = new WhileNode(support.union($<Token>1.getPosition(), getPosition($<ISourcePositionHolder>1)), support.getConditionNode($3), $6);
					$$ = support.introduceComment(node, new Object[]{$1, $<>4, $7});
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_UNTIL);
			    } */
                }
              | kUNTIL {
                    lexer.getConditionState().begin();
                } expr_value do {
                    lexer.getConditionState().end();
                } compstmt kEND {
                    UntilNode node = new UntilNode(getPosition($<ISourcePositionHolder>1), support.getConditionNode($3), $6);
                    $$ = support.introduceComment(node, new Object[]{$1, $<>4, $7});
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_WHILE);
			    } */
                }
              | kCASE expr_value opt_terms 
		case_body 
                kEND {
		    			CaseNode node = new CaseNode(getPosition($<ISourcePositionHolder>1), $2, $4); // XXX
		    			$$ = support.introduceComment(node, new Object[]{$1, $<>3, $5});
                }
              | kCASE opt_terms case_body kEND {
                    CaseNode node = new CaseNode(getPosition($<ISourcePositionHolder>1), null, $3);
                    $$ = support.introduceComment(node, new Object[]{$1, $<>2, $4});
                }
              | kCASE opt_terms kELSE compstmt kEND {
		    			
		    			$$ = support.introduceComment($4, new Object[]{$1, $<>2, $3, $5});
                }
              | kFOR block_var kIN {
                    lexer.getConditionState().begin();
                } expr_value do {
                    lexer.getConditionState().end();
                } compstmt 
                  kEND {
                    ForNode node = new ForNode(getPosition($<ISourcePositionHolder>1), $2, $8, $5);
                    $$ = support.introduceComment(node, new Object[]{$1, $3, $<>6, $9});
                }
              | kCLASS cpath superclass term{
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("class definition in method body");
                    }
                    support.getLocalNames().push(new LocalNamesElement());
                    // $$ = new Integer(ruby.getSourceLine());
                } bodystmt 
		  kEND {
		  			Object[] tokens = new Object[]{$<>4};
		  			
		  			ClassNode classNode;
		  			
		  			if($3 == null){
		  				classNode = new ClassNode(support.union($1, $7), support.introduceComment($2, tokens), new ScopeNode(getRealPosition($6), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), $6), $3);
                   
		  			} else {
		  				classNode = new ClassNode(support.union($1, $7), $2, new ScopeNode(getRealPosition($6), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), $6), support.introduceComment($3, tokens));
                   
		  			}
		  			
                     
					tokens = new Object[]{$1,$7};
                    $$ = support.introduceComment(classNode, tokens);
                    // $<Node>$.setLine($<Integer>4.intValue());
                    support.getLocalNames().pop();
                }
              | kCLASS tLSHFT expr {
                    $$ = new Boolean(support.isInDef());
                    support.setInDef(false);
                } term {
                    $$ = new Integer(support.getInSingle());
                    support.setInSingle(0);
                    support.getLocalNames().push(new LocalNamesElement());
                } bodystmt 
                  kEND {
                    SClassNode node = new SClassNode(support.union($1, $8), $3, new ScopeNode(getRealPosition($7), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), $7));
                    $$ = support.introduceComment(node, new Object[]{$1, $2, $<>5, $8});
                    support.getLocalNames().pop();
                    support.setInDef($<Boolean>4.booleanValue());
                    support.setInSingle($<Integer>6.intValue());
                }
              | kMODULE cpath {
                    if (support.isInDef() || support.isInSingle()) { 
                        yyerror("module definition in method body");
                    }
                    support.getLocalNames().push(new LocalNamesElement());
                    // $$ = new Integer(ruby.getSourceLine());
                } bodystmt 
                  kEND {
                    ModuleNode node = new ModuleNode(support.union($1, $5), $2, new ScopeNode(getRealPosition($4), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), $4));
                    $$ = support.introduceComment(node, new Object[]{$1, $5});
                    // $<Node>$.setLine($<Integer>3.intValue());
                    support.getLocalNames().pop();
                }
	      | kDEF fname {
		      /* missing
			$<id>$ = cur_mid;
			cur_mid = $2; */
                    support.setInDef(true);
                    support.getLocalNames().push(new LocalNamesElement());
                } f_arglist 
                  bodystmt 
                  kEND {
		      /* was in old jruby grammar support.getClassNest() !=0 || IdUtil.isAttrSet($2) ? Visibility.PUBLIC : Visibility.PRIVATE); */
                    /* NOEX_PRIVATE for toplevel */
                    
                    Object[] tokens = new Object[]{$1, $2, $6};
                    
                    DefnNode node = new DefnNode(support.union($1, $6), new ArgumentNode($<ISourcePositionHolder>2.getPosition(), (String) $2.getValue()), $4,
		                      new ScopeNode(getRealPosition($5), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), $5), Visibility.PRIVATE);
                    
                    
                    $$ = support.introduceComment(node, tokens);
                    // $<Node>$.setPosFrom($4);
                    support.getLocalNames().pop();
                    support.setInDef(false);
		    /* missing cur_mid = $<id>3; */
                }
              | kDEF singleton dot_or_colon {
                    lexer.setState(LexState.EXPR_FNAME);
                } fname {
                    support.setInSingle(support.getInSingle() + 1);
                    support.getLocalNames().push(new LocalNamesElement());
                    lexer.setState(LexState.EXPR_END); /* force for args */
                } f_arglist 
		  bodystmt 
                  kEND {
                  	DefsNode node = new DefsNode(support.union($1, $9), $2, (String) $5.getValue(), $7, new ScopeNode(getPosition(null), ((LocalNamesElement) support.getLocalNames().peek()).getNamesArray(), $8));
                    
                    $$ = support.introduceComment(node, new Object[]{$1, $3, $5, $9});
                    // $<Node>$.setPosFrom($2);
                    support.getLocalNames().pop();
                    support.setInSingle(support.getInSingle() - 1);
                }
              | kBREAK {
                    BreakNode node = new BreakNode(getPosition($<ISourcePositionHolder>1));
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | kNEXT {
                    NextNode node = new NextNode(getPosition($<ISourcePositionHolder>1));
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | kREDO {
              		RedoNode node = new RedoNode(getPosition($<ISourcePositionHolder>1));
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | kRETRY {
                    RetryNode node = new RetryNode(getPosition($<ISourcePositionHolder>1));
                    $$ = support.introduceComment(node, new Object[]{$1});
                }

primary_value : primary {
                    support.checkExpression($1);
		    $$ = $1;
		}
 
then          : term
              | ":"
              | kTHEN
              | term kTHEN{
              	    $<Token>1.addComments($<Token>2.getComments());
              }

do            : term
              | ":"
              | kDO_COND

if_tail       : opt_else 
              | kELSIF expr_value then 
                compstmt 
                if_tail {
               		IfNode node = new IfNode(support.union($<ISourcePositionHolder>1.getPosition(), getPosition($<ISourcePositionHolder>1)), support.getConditionNode($2), $4, $5);
               		$$ = support.introduceComment(node, new Object[]{$1, $<>3});
                }

opt_else      : none 
              | kELSE compstmt {
                    $$ = support.introduceComment($2, new Object[]{$1});
                }

block_var     : lhs
              | mlhs {}

opt_block_var : none
              | tPIPE /* none */ tPIPE {
                    ZeroArgNode node = new ZeroArgNode(getPosition(null));
                    $$ = support.introduceComment(node, new Object[]{$1, $2});
                }
              | tOROP {
                    ZeroArgNode node = new ZeroArgNode(getPosition(null));
                    $$ = support.introduceComment(node, new Object[]{$1});
		}
              | tPIPE block_var tPIPE {
                    $$ = support.introduceComment($2, new Object[]{$1, $3});
                }

do_block      : kDO_BLOCK {
                    support.getBlockNames().push(new BlockNamesElement());
		} opt_block_var compstmt 
		  kEND {
                    $$ = new IterNode(getPosition($<ISourcePositionHolder>1), $3, support.introduceComment($4, new Object[]{$1, $5}), null);
                   
                    support.getBlockNames().pop();
                }

block_call    : command do_block {
                    if ($1 instanceof BlockPassNode) {
                        throw new SyntaxException(getPosition($<ISourcePositionHolder>1), "Both block arg and actual block given.");
                    }
                    $2.setIterNode($1);
                    $$ = $2;
                }
              | block_call tDOT operation2 opt_paren_args {
                    Node node = support.new_call($1, $3, $4);
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
                }
              | block_call tCOLON2 operation2 opt_paren_args {
                    Node node = support.new_call($1, $3, $4);
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
                }

method_call   : operation paren_args {
                    Node node = support.new_fcall((String) $1.getValue(), $2, $1); // .setPosFrom($2);
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | primary_value tDOT operation2 opt_paren_args {
                    Node node = support.new_call($1, $3, $4); //.setPosFrom($1);
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
                }
              | primary_value tCOLON2 operation2 paren_args {
                    Node node = support.new_call($1, $3, $4); //.setPosFrom($1);
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
                }
              | primary_value tCOLON2 operation3 {
              		Node node = support.new_call($1, $3, null);
                    $$ = support.introduceComment(node, new Object[]{$2, $3});
                }
              | kSUPER paren_args {
              		Node node = support.new_super($2, $1);
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | kSUPER {
                    	ZSuperNode node = new ZSuperNode(getPosition($<ISourcePositionHolder>1));
                    $$ = support.introduceComment(node, new Object[]{$1});
                }

brace_block   : tLCURLY {
                    support.getBlockNames().push(new BlockNamesElement());
		} opt_block_var compstmt '}' {
                    $$ = new IterNode(getPosition($<ISourcePositionHolder>1), $3, support.introduceComment($4, new Object[]{$1, $<>5}), null);
                    support.getBlockNames().pop();
                }
              | kDO {
                    support.getBlockNames().push(new BlockNamesElement());
		} opt_block_var compstmt kEND {
                    $$ = new IterNode(support.union($<Token>1.getPosition(), getPosition($<ISourcePositionHolder>1)), $3, support.introduceComment($4, new Object[]{$1, $5}), null);
                    support.getBlockNames().pop();
                }

case_body     : kWHEN when_args then 
                compstmt 
		cases {
		    			WhenNode node = new WhenNode(getPosition($<ISourcePositionHolder>1), $2, $4, $5);
		    			$$ = support.introduceComment(node, new Object[]{$1, $<>3});
                }

when_args     : args
              | args ',' tSTAR arg_value {
                    support.commentLastElement($1, new Object[]{$<>2});
                    $$ = $1.add(new WhenNode(getPosition($<ISourcePositionHolder>1), support.introduceComment($4, new Object[]{$3}), null, null));
                }
              | tSTAR arg_value {
                    $$ = new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(new WhenNode(getPosition($<ISourcePositionHolder>1), support.introduceComment($2, new Object[]{$1}), null, null));
                }

cases         : opt_else 
              | case_body


opt_rescue    : kRESCUE exc_list exc_var then
		compstmt
		opt_rescue {
                    Node node;
		    if ($3 != null) {
                       node = support.appendToBlock(support.node_assign($3, new GlobalVarNode(getPosition($<ISourcePositionHolder>1), "$!")), support.introduceComment($5, new Object[]{$1, $<>4}));
		    } else {
		       node = $5;
                    }
                    $$ = new RescueBodyNode(getPosition($<ISourcePositionHolder>1, true), $2, node, $6);
		}
              | {$$ = null;}

exc_list      : arg_value {
                    $$ = new ArrayNode($<ISourcePositionHolder>1.getPosition()).add($1);
		}
              | mrhs
	      | none

exc_var       : tASSOC lhs {
                    $$ = support.introduceComment($2, new Object[]{$1});
                }
              | none

opt_ensure    : kENSURE compstmt {
                    if ($2 != null) {
                        $$ = $2;
                    } else {
                        $$ = new NilNode(getPosition(null));
                    }
                    $$ = support.introduceComment($<Node>$, new Object[]{$1});
                }
              | none

literal       : numeric
              | symbol {
              		SymbolNode node = new SymbolNode($<ISourcePositionHolder>1.getPosition(), (String) $1.getValue());
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | dsym

strings       : string {
		    if ($1 == null) {
		        $$ = new StrNode(getPosition($1), "");
		    } else {
		        if ($1 instanceof EvStrNode) {
			    $$ = new DStrNode(getPosition($<ISourcePositionHolder>1)).add($1);
			} else {
		            $$ = $1;
			}
		    }
		} 

string        : string1 {
                    $$ = support.literal_concat(getPosition($<ISourcePositionHolder>1), null, $1);
		}
              | string string1 {
                    $$ = support.literal_concat(getPosition($<ISourcePositionHolder>1), $1, $2);
		}

string1       : tSTRING_BEG string_contents tSTRING_END {
		     $$ = support.introduceComment($2, new Object[]{$1, $3});
		}
//tcorbat: why creating a new SelfNode here? Might loose the comment here.
xstring	      : tXSTRING_BEG xstring_contents tSTRING_END {
		    if ($2 == null) {
			  $$ = new XStrNode(getPosition($<ISourcePositionHolder>1),null);
		    } else {
		      if ($2 instanceof StrNode) {
			  $$ = new XStrNode(getPosition($<ISourcePositionHolder>1), $<StrNode>2.getValue());
		      } else if ($2 instanceof DStrNode) {
			  $$ = new DXStrNode(getPosition($<ISourcePositionHolder>1)).add($2);
		      } else {
			$$ = new DXStrNode(getPosition($<ISourcePositionHolder>1)).add(new ArrayNode(getPosition($<ISourcePositionHolder>1)).add($2));
		      }
		    }
		    		$$ = support.introduceComment($<Node>$, new Object[]{$1, $3});
                }

regexp	      : tREGEXP_BEG xstring_contents tREGEXP_END {
		    int options = ((RegexpNode)$3.getValue()).getOptions();
		    Node node = $2;

		    if (node == null) {
		        $$ = new RegexpNode(getPosition($<ISourcePositionHolder>1), "", options & ~ReOptions.RE_OPTION_ONCE);
		    } else if (node instanceof StrNode) {
		      $$ = new RegexpNode($<Node>2.getPosition(), ((StrNode) node).getValue(), options & ~ReOptions.RE_OPTION_ONCE);
		    } else {
		        if (node instanceof DStrNode == false) {
			    node = new DStrNode(getPosition($<ISourcePositionHolder>1)).add(new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(node));
		        } 

			$$ = new DRegexpNode(getPosition($<ISourcePositionHolder>1), options, (options & ReOptions.RE_OPTION_ONCE) != 0).add(node);
		    }
		    $$ = support.introduceComment($<Node>$, new Object[]{$1, $3});
		 }

words	       : tWORDS_BEG ' ' tSTRING_END {
		     ZArrayNode node = new ZArrayNode(getPosition($<ISourcePositionHolder>1));
		     $$ = support.introduceComment(node, new Object[]{$1, $<>2, $3});
		 }
	       | tWORDS_BEG word_list tSTRING_END {
		     $$ = support.introduceComment($2, new Object[]{$1, $3});
		 }

word_list      : /* none */ {
		     $$ = null;
		 }
	       | word_list word ' ' {
                     Node node = $2;

                     if (node instanceof EvStrNode) {
		       node = new DStrNode(getPosition($<ISourcePositionHolder>1)).add(node);
		     }

		     if ($1 == null) {
		       $$ = new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(node);
		     } else {
		       $$ = $1.add(node);
		     }
		 }

word	       : string_content
	       | word string_content {
                     $$ = support.literal_concat(getPosition($<ISourcePositionHolder>1), $1, $2);
	         }

qwords	       : tQWORDS_BEG ' ' tSTRING_END {
		     ZArrayNode node = new ZArrayNode(getPosition($<ISourcePositionHolder>1));
		     $$ = support.introduceComment(node, new Object[]{$1, $<>2, $3});
		 }
	       | tQWORDS_BEG qword_list tSTRING_END {
		     $$ = support.introduceComment($2, new Object[]{$1, $3});
		 }

qword_list     : /* none */ {
		     $$ = null;
		 }
	       | qword_list tSTRING_CONTENT ' ' {
                     if ($1 == null) {
		         $$ = new ArrayNode(getPosition($<ISourcePositionHolder>1)).add(new StrNode($<ISourcePositionHolder>2.getPosition(), (String) $2.getValue()));
		     } else {
		         $$ = $1.add(new StrNode($<ISourcePositionHolder>2.getPosition(), (String) $2.getValue()));
		     }
		 }

string_contents : /* none */ {
		     $$ = null;
		 }
		| string_contents string_content {
                     $$ = support.literal_concat(getPosition($<ISourcePositionHolder>1), $1, $2);
		 }

xstring_contents: /* none */ {
		     $$ = null;
		 }
		| xstring_contents string_content {
                     $$ = support.literal_concat(getPosition($<ISourcePositionHolder>1), $1, $2);
		 }


string_content	: tSTRING_CONTENT {
						StrNode node = new StrNode($<ISourcePositionHolder>1.getPosition(), (String) $1.getValue());
                      $$ = support.introduceComment(node, new Object[]{$1});
                  }
		| tSTRING_DVAR {
                      $$ = lexer.getStrTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  } string_dvar {
		      lexer.setStrTerm($<StrTerm>2);
		      EvStrNode node = new EvStrNode(getPosition($<ISourcePositionHolder>1), $3);
		      $$ = support.introduceComment(node, new Object[]{$1});
		  }
		| tSTRING_DBEG {
		      $$ = lexer.getStrTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  } compstmt '}' {
		      lexer.setStrTerm($<StrTerm>2);
		      Node node = $3;

		      if (node instanceof NewlineNode) {
		        node = ((NewlineNode)node).getNextNode();
		      }
				Node evNode = support.newEvStrNode(getPosition($<ISourcePositionHolder>3, true), node);
		      $$ = support.introduceComment(evNode, new Object[]{$1, $<>4});
		  }

string_dvar    : tGVAR {
                      $$ = new GlobalVarNode(getPosition($<ISourcePositionHolder>1), (String) $1.getValue());
                 }
	       | tIVAR {
                      $$ = new InstVarNode(getPosition($<ISourcePositionHolder>1), (String) $1.getValue());
                 }
	       | tCVAR {
                      $$ = new ClassVarNode(getPosition($<ISourcePositionHolder>1), (String) $1.getValue());
                 }
	       | backref


symbol        : tSYMBEG sym {
                    lexer.setState(LexState.EXPR_END);
                    $2.addComments($1.getComments());
                    $$ = $2;
		    $<ISourcePositionHolder>$.setPosition(support.union($1, $2));
                }

sym           : fname
              | tIVAR
              | tGVAR
              | tCVAR

dsym	      : tSYMBEG xstring_contents tSTRING_END {
                    lexer.setState(LexState.EXPR_END);

		    // In ruby, it seems to be possible to get a
		    // StrNode (NODE_STR) among other node type.  This 
		    // is not possible for us.  We will always have a 
		    // DStrNode (NODE_DSTR).
		    if($2 instanceof StrNode){
		    	DStrNode n = new DStrNode($<StrNode>2.getPosition());
			n.add($<StrNode>2);
			$$ = new DSymbolNode(getPosition($<ISourcePositionHolder>1), n);
		    }else{
		    $$ = new DSymbolNode(getPosition($<ISourcePositionHolder>1), $<DStrNode>2);
		}
		    	$$ = support.introduceComment($<Node>$, new Object[]{$1, $3});
		}

numeric       : tINTEGER {
                    Object number = $1.getValue();

                    if (number instanceof Long) {
		        $$ = new FixnumNode($<ISourcePositionHolder>1.getPosition(), ((Long) number).longValue());
                    } else {
		        $$ = new BignumNode($<ISourcePositionHolder>1.getPosition(), (BigInteger) number);
                    }
                    $$ = support.introduceComment($<Node>$, new Object[]{$1});
                }
              | tFLOAT {
                    FloatNode node = new FloatNode($<ISourcePositionHolder>1.getPosition(), ((Double) $1.getValue()).doubleValue());
                    $$ = support.introduceComment(node, new Object[]{$1});
	        }
	      | tUMINUS_NUM tINTEGER	       %prec tLOWEST {
                    Object number = $2.getValue();
					Node node = support.getOperatorCallNode((number instanceof Long ? (Node) new FixnumNode(getPosition($<ISourcePositionHolder>1), ((Long) number).longValue()) : (Node) new BignumNode(getPosition($<ISourcePositionHolder>1), (BigInteger) number)), "-@");
                    $$ = support.introduceComment(node, new Object[]{$1, $2});
		}
	      | tUMINUS_NUM tFLOAT	       %prec tLOWEST {
                    Node node = support.getOperatorCallNode(new FloatNode(getPosition($<ISourcePositionHolder>1), ((Double) $2.getValue()).doubleValue()), "-@");
                    $$ = support.introduceComment(node, new Object[]{$1, $2});
		}

		  /* Enebo: Now that variable is either a String or an Node
		     All users of variable production must be examined
		     to make sure we can cast to String or not...I am
		     unsure.
		   */
variable      : tIDENTIFIER {
                    $$ = $1;
                }
              | tIVAR {
                    $$ = $1;
                }
              | tGVAR {
                    $$ = $1;
                }
              | tCONSTANT {
                    $$ = $1;
                }
	      | tCVAR {
                    $$ = $1;
                }
              | kNIL { 
              		NilNode node = new NilNode($<ISourcePositionHolder>1.getPosition());
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | kSELF {
              		SelfNode node = new SelfNode($<ISourcePositionHolder>1.getPosition());
                    $$ = support.introduceComment(node, new Object[]{$1});
                }
              | kTRUE { 
              		TrueNode node = new TrueNode($<ISourcePositionHolder>1.getPosition());
		    			$$ = support.introduceComment(node, new Object[]{$1});
                }
              | kFALSE {
              		FalseNode node = new FalseNode($<ISourcePositionHolder>1.getPosition());
		    			$$ = support.introduceComment(node, new Object[]{$1});
                }
              | k__FILE__ {
                    StrNode node = new StrNode($<ISourcePositionHolder>1.getPosition(), getPosition($<ISourcePositionHolder>1).getFile());
                    $$ = support.introduceComment(node, new Object[]{$1}); 
                    
                }
              | k__LINE__ {
              		FixnumNode node = new FixnumNode(getPosition($<ISourcePositionHolder>1), getPosition($<ISourcePositionHolder>1).getEndLine() + 1);
                    $$ = support.introduceComment(node, new Object[]{$1});
                }

var_ref       : variable {
                    // Work around __LINE__ and __FILE__ 
                    if ($1 instanceof INameNode) {
		        String name = $<INameNode>1.getName();
                        $$ = support.gettable(name, $<ISourcePositionHolder>1.getPosition());
		    } else if ($1 instanceof Token) {
		      $$ = support.gettable((String) $<Token>1.getValue(), $<ISourcePositionHolder>1.getPosition());
		    } else {
		        $$ = $1;
		    }
				    $$ = support.introduceComment($<Node>$, new Object[]{$1});
                }


var_lhs	      : variable {
                    Node node = support.assignable(getPosition($<ISourcePositionHolder>1), $1, null);
                    $$ = support.introduceComment(node, new Object[]{$1});
                }

backref       : tNTH_REF 
				{
					$$ = support.introduceComment((Node)$1.getValue(), new Object[]{$1});
					
				}
              | tBACK_REF
              	{
					$$ = support.introduceComment((Node)$1.getValue(), new Object[]{$1});
				}

superclass    : {
                    $$ = null;
                }
              | tLT {
                    lexer.setState(LexState.EXPR_BEG);
                } expr_value {
                		
                    $$ = support.introduceComment($3, new Object[]{$1});
                }
              | error {
                    yyerrok();
                    $$ = null;
                }

f_arglist     : tLPAREN2 f_args opt_nl ')' {
                    Object[] tokens = new Object[]{$1, $<>3 ,$<>4};
                                                            
                    $$ = support.introduceComment($2, tokens);
                    lexer.setState(LexState.EXPR_BEG);
                }
              | f_args term {
              		Object[] tokens = new Object[]{$<>2};
              		$$ = support.introduceComment($1, tokens);
                }

f_args        : f_arg ',' f_optarg ',' f_rest_arg opt_f_block_arg {
                    ListNode argLNode = support.commentLastElement($1, new Object[]{$<>2});
                    ListNode optLNode = support.commentLastElement($3, new Object[]{$<>4});
                    ArgsNode node = new ArgsNode(getPosition($<ISourcePositionHolder>1), $1, $3, ((Integer) $5.getValue()).intValue(), $6);
                    
                    $$ = support.introduceComment(node, new Object[]{$5});
                }
              | f_arg ',' f_optarg opt_f_block_arg {
                    Object[] tokens = new Object[]{$<>2};
                    ListNode lNode = support.commentLastElement($1, tokens);
                    $$ = new ArgsNode(getPosition($<ISourcePositionHolder>1), lNode, $3, -1, $4);
                }
                
                
              | f_arg ',' f_rest_arg opt_f_block_arg {
                    Object[] tokens = new Object[]{$<>2};
                    ListNode lNode = support.commentLastElement($1, tokens);
                    
                    tokens = new Object[]{$3};
                    ArgsNode node = new ArgsNode(getPosition($<ISourcePositionHolder>1), lNode, null, ((Integer) $3.getValue()).intValue(), $4);
                     
                    $$ = support.introduceComment(node, tokens);
                }
                
              | f_arg opt_f_block_arg {
                    $$ = new ArgsNode($<ISourcePositionHolder>1.getPosition(), $1, null, -1, $2);
                }
              | f_optarg ',' f_rest_arg opt_f_block_arg {
                    Object[] tokens = new Object[]{$<>2};
                    
                    ListNode lNode = support.commentLastElement($1,tokens);
                    
                    tokens = new Object[]{$3};                    
                    ArgsNode node = new ArgsNode(getPosition($<ISourcePositionHolder>1), null, lNode, ((Integer) $3.getValue()).intValue(), $4);
                    $$ = support.introduceComment(node, tokens);
                }
              | f_optarg opt_f_block_arg {
                    $$ = new ArgsNode(getPosition($<ISourcePositionHolder>1), null, $1, -1, $2);
                }
              | f_rest_arg opt_f_block_arg {
             		Object[] tokens = new Object[]{$1};
             		ArgsNode node = new ArgsNode(getPosition($<ISourcePositionHolder>1), null, null, ((Integer) $1.getValue()).intValue(), $2);
                    $$ = support.introduceComment(node, tokens);
                      
                }
              | f_block_arg {
                    $$ = new ArgsNode(getPosition($<ISourcePositionHolder>1), null, null, -1, $1);
                }
              | /* none */ {
	      	   //take the last position from the lexer, this isn't entirely correct, but more correct then getPosition(null).
                   $$ = new ArgsNode(lexer.getPosition(), null, null, -1, null);
                }

f_norm_arg    : tCONSTANT {
                    yyerror("formal argument cannot be a constant");
                }
              | tIVAR {
                    yyerror("formal argument cannot be an instance variable");
                }
              | tCVAR {
                    yyerror("formal argument cannot be a class variable");
                }
              | tIDENTIFIER {
                   String identifier = (String) $1.getValue();
                   if (!IdUtil.isLocal(identifier)) {
                        yyerror("formal argument must be local variable");
                    } else if (((LocalNamesElement) support.getLocalNames().peek()).isLocalRegistered(identifier)) {
                        yyerror("duplicate argument name");
                    }
		    // Register new local var or die trying (side-effect)
                    ((LocalNamesElement) support.getLocalNames().peek()).getLocalIndex(identifier);
                    $$ = $1;
                }

f_arg         : f_norm_arg {
					ArgumentNode node = new ArgumentNode($<ISourcePositionHolder>1.getPosition(), (String) $1.getValue());
                    $$ = new ListNode($<ISourcePositionHolder>1.getPosition());
                    ((ListNode) $$).add(support.introduceComment(node, new Object[]{$1}));
                }
              | f_arg ',' f_norm_arg {
                    ArgumentNode node = new ArgumentNode($<ISourcePositionHolder>3.getPosition(), (String) $3.getValue());
                    
                    support.commentLastElement($1, new Object[]{$<>2});
                    
                    $1.add(support.introduceComment(node, new Object[]{$3}));
                    $1.setPosition(support.union($1, $3));
		    $$ = $1;
                }

f_opt         : tIDENTIFIER '=' arg_value {
                    String identifier = (String) $1.getValue();

                    if (!IdUtil.isLocal(identifier)) {
                        yyerror("formal argument must be local variable");
                    } else if (((LocalNamesElement) support.getLocalNames().peek()).isLocalRegistered(identifier)) {
                        yyerror("duplicate optional argument name");
                    }
		    ((LocalNamesElement) support.getLocalNames().peek()).getLocalIndex(identifier);
                    
                    Node node = support.assignable(getPosition($<ISourcePositionHolder>1), identifier, $3);
                    $$ = support.introduceComment(node, new Object[]{$1, $<>2});
                }

f_optarg      : f_opt {
                    $$ = new BlockNode(getPosition($<ISourcePositionHolder>1)).add($1);
                }
              | f_optarg ',' f_opt {
                    support.commentLastElement($1, new Object[]{$<>2});
                    $$ = support.appendToBlock($1, $3);
                }

 restarg_mark	: tSTAR2 
                | tSTAR

f_rest_arg    : restarg_mark tIDENTIFIER {
                    String identifier = (String) $2.getValue();

                    if (!IdUtil.isLocal(identifier)) {
                        yyerror("rest argument must be local variable");
                    } else if (((LocalNamesElement) support.getLocalNames().peek()).isLocalRegistered(identifier)) {
                        yyerror("duplicate rest argument name");
                    }
		    $1.setValue(new Integer(((LocalNamesElement) support.getLocalNames().peek()).getLocalIndex(identifier)));
                    $$ = $1;
                }
              | restarg_mark {
                    $1.setValue(new Integer(-2));
                    $$ = $1;
                }

blkarg_mark	: tAMPER2
		| tAMPER

f_block_arg   : blkarg_mark tIDENTIFIER {
                    String identifier = (String) $2.getValue();

                    if (!IdUtil.isLocal(identifier)) {
                        yyerror("block argument must be local variable");
                    } else if (((LocalNamesElement) support.getLocalNames().peek()).isLocalRegistered(identifier)) {
                        yyerror("duplicate block argument name");
                    }
                    $$ = new BlockArgNode(getPosition($<ISourcePositionHolder>1), ((LocalNamesElement) support.getLocalNames().peek()).getLocalIndex(identifier));
                }

opt_f_block_arg: ',' f_block_arg {
                    $$ = $2;
                }
              | /* none */ {
	            $$ = null;
	        }

singleton     : var_ref {
                    if ($1 instanceof SelfNode) {
		        $$ = new SelfNode($<ISourcePositionHolder>1.getPosition());
                    } else {
			support.checkExpression($1);
			$$ = $1;
		    }
                }
              | tLPAREN2 {
                    lexer.setState(LexState.EXPR_BEG);
                } expr opt_nl ')' {
                    if ($3 instanceof ILiteralNode) {
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
		    support.checkExpression($3);
		    
		    			$$ = support.introduceComment($3, new Object[]{$1, $<>4, $<>5});
                }

assoc_list    : none_list
              | assocs trailer {
                    $$ = support.commentLastElement($1, new Object[]{$<>2});
                }
              | args trailer {
                    if ($1.size() % 2 != 0) {
                        yyerror("Odd number list for Hash.");
                    }
                    $$ = support.commentLastElement($1, new Object[]{$<>2});
                }

assocs        : assoc
              | assocs ',' assoc {
              		support.commentLastElement($1, new Object[]{$<>2});
                    $$ = $1.addAll($3);
                }

assoc         : arg_value tASSOC arg_value {
					Node node = support.introduceComment($1, new Object[]{$2});
                    $$ = new ArrayNode(support.union($1, $3)).add(node).add($3);
                }

operation     : tIDENTIFIER
              | tCONSTANT
              | tFID

operation2    : tIDENTIFIER
              | tCONSTANT
              | tFID
              | op

operation3    : tIDENTIFIER
              | tFID
              | op

dot_or_colon  : tDOT
              | tCOLON2

opt_terms     : /* none */
              | terms

opt_nl        : /* none */
              | '\n'

trailer       : /* none */
              | '\n'
              | ','

term          : ';' {
                    yyerrok();
                }
              | '\n'

terms         : term
              | terms ';' {
                    yyerrok();
                    $<Token>1.addComments($<Token>2.getComments());
                }

none          : /* none */ {
                    $$ = null;
                }

none_list     : {  $$ = null;
		  }

none_block_pass     : {  $$ = null;
		  }

opt_eof_comment	:
				| tEOF_COMMENT

%%


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
