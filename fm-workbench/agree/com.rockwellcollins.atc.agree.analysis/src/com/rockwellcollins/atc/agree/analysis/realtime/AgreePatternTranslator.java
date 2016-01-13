package com.rockwellcollins.atc.agree.analysis.realtime;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osate.aadl2.instance.ComponentInstance;

import com.rockwellcollins.atc.agree.agree.OpenTimeInterval;
import com.rockwellcollins.atc.agree.agree.TimeInterval;
import com.rockwellcollins.atc.agree.analysis.AgreeException;
import com.rockwellcollins.atc.agree.analysis.ast.AgreeNode;
import com.rockwellcollins.atc.agree.analysis.ast.AgreeNodeBuilder;
import com.rockwellcollins.atc.agree.analysis.ast.AgreeProgram;
import com.rockwellcollins.atc.agree.analysis.ast.AgreeStatement;
import com.rockwellcollins.atc.agree.analysis.ast.AgreeVar;
import com.rockwellcollins.atc.agree.analysis.realtime.AgreePattern.TriggerType;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.RealExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;

public class AgreePatternTranslator {

    private static final String TIME_PREFIX = "__TIME__";
    private static final String EFFECT_PREFIX = "__EVENT__";
    private static final String CAUSE_PREFIX = "__CAUSE__";
    private static final String IN_INTERVAL_PREFIX = "__IN_INTERVAL__";
    private static final String EFFECT_TIME_RANGE_PREFIX = "__EFFECT_TIME_RANGE__";
    private static final String EFFECT_TIME_INTERVAL_END = "__EFFECT_TIME_INTERVAL_END__";
    private static final String CAUSE_CONDITION_EVENT_PREFIX = "__CAUSE_CONDITION_HELD__";
    private static final String CAUSE_CONDITION_START_PREFIX = "__CAUSE_CONDITION_START__";
    private static final String CAUSE_CONDITION_END_PREFIX = "__CAUSE_CONDITION_END__";
    
    private static final Expr NEG_ONE = new RealExpr(BigDecimal.valueOf(-1));

    private static int eventIndex;
    private static int patternIndex;
    private static List<Node> patternLustreNodes;
    private static Map<AgreeNode, List<AgreeVar>> nodeEvents;
    public static final IdExpr timeExpr = new IdExpr("time");

    public static AgreeProgram translate(AgreeProgram program) {
        patternLustreNodes = new ArrayList<>();
        nodeEvents = new HashMap<>();

        AgreeNode topNode = translateNode(program.topNode, true);
        List<AgreeNode> agreeNodes = gatherNodes(topNode);
        patternLustreNodes.addAll(program.globalLustreNodes);

        return new AgreeProgram(agreeNodes, patternLustreNodes, program.globalTypes, topNode);
    }

    private static AgreeNode translateNode(AgreeNode node, boolean isTopNode) {
        eventIndex = 0;
        patternIndex = 0;
        AgreeNodeBuilder builder = new AgreeNodeBuilder(node);

        // this has to be done first because the pattern translation
        // for guarantees/lemmas/assumptions add additional assertions
        builder.clearAssertions();
        for (AgreeStatement statement : node.assertions) {
            if (statement instanceof AgreePattern) {
                Expr transExpr = translatePattern((AgreePattern) statement, builder, !isTopNode);
                statement = new AgreeStatement(statement.string, transExpr, statement.reference);
            }
            builder.addAssertion(statement);
        }

        builder.clearGuarantees();
        for (AgreeStatement statement : node.guarantees) {
            if (statement instanceof AgreePattern) {
                Expr transExpr = translatePattern((AgreePattern) statement, builder, isTopNode);
                statement = new AgreeStatement(statement.string, transExpr, statement.reference);
            }
            builder.addGuarantee(statement);
        }

        builder.clearLemmas();
        for (AgreeStatement statement : node.lemmas) {
            if (statement instanceof AgreePattern) {
                Expr transExpr = translatePattern((AgreePattern) statement, builder, isTopNode);
                statement = new AgreeStatement(statement.string, transExpr, statement.reference);
            }
            builder.addLemma(statement);
        }

        builder.clearAssumptions();
        for (AgreeStatement statement : node.assumptions) {
            if (statement instanceof AgreePattern) {
                Expr transExpr = translatePattern((AgreePattern) statement, builder, !isTopNode);
                statement = new AgreeStatement(statement.string, transExpr, statement.reference);
            }
            builder.addAssumption(statement);
        }

        builder.clearSubNodes();
        for (AgreeNode subNode : node.subNodes) {
            builder.addSubNode(translateNode(subNode, false));
        }

        builder.addInput(new AgreeVar(timeExpr.id, NamedType.REAL, null, node.compInst));
        return builder.build();
    }

    private static List<AgreeNode> gatherNodes(AgreeNode node) {
        List<AgreeNode> nodes = new ArrayList<>();

        for (AgreeNode subNode : node.subNodes) {
            nodes.addAll(gatherNodes(subNode));
            nodes.add(subNode);
        }
        return nodes;
    }
    
    

    private static Expr translatePattern(AgreePattern pattern, AgreeNodeBuilder builder, boolean isProperty) {

        patternIndex++;
        AgreeNode curNode = builder.build();
        ComponentInstance compInst = curNode.compInst;

        if (pattern instanceof AgreeTimesPattern) {
            return translatePattern((AgreeTimesPattern) pattern, builder);
        }

        AgreeVar causeVar = new AgreeVar(CAUSE_PREFIX + eventIndex, NamedType.BOOL, null, compInst);
        AgreeVar effectVar = new AgreeVar(EFFECT_PREFIX + eventIndex++, NamedType.BOOL, null, compInst);
        AgreeVar timeCause = new AgreeVar(TIME_PREFIX + causeVar.id, NamedType.REAL, null, compInst);
        AgreeVar timeEffect = new AgreeVar(TIME_PREFIX + effectVar.id, NamedType.REAL, null, compInst);
//        AgreeVar inEffectInterval =
//                new AgreeVar(IN_INTERVAL_PREFIX + effectVar.id, NamedType.BOOL, null, compInst);
        AgreeVar effectTimeRangeVar =
                new AgreeVar(EFFECT_TIME_RANGE_PREFIX + patternIndex, NamedType.REAL, null, compInst);
        AgreeVar effectTimeIntervalEndVar =
                new AgreeVar(EFFECT_TIME_INTERVAL_END + patternIndex, NamedType.REAL, null, compInst);

        IdExpr causeId = new IdExpr(causeVar.id);
        IdExpr effectId = new IdExpr(effectVar.id);
        IdExpr timeCauseId = new IdExpr(timeCause.id);
        IdExpr effectTimeRangeId = new IdExpr(effectTimeRangeVar.id);
        IdExpr timeEffectId = new IdExpr(timeEffect.id);
        IdExpr effectTimeIntervalEndId = new IdExpr(effectTimeIntervalEndVar.id);
        //IdExpr inEffectIntervalId = new IdExpr(inEffectInterval.id);

        builder.addLocal(causeVar);
        builder.addLocal(effectVar);
        builder.addOutput(timeEffect);

        builder.addLocalEquation(new Equation(causeId, pattern.cause));
        builder.addLocalEquation(new Equation(effectId, pattern.effect));
        
        if(pattern.causeType == TriggerType.CONDITION){
            causeId = translateCauseCondtionPattern(causeId, pattern.causeInterval, builder);
        }
        
        
        if (isProperty) {
            builder.addLocal(timeCause);
            // builder.addLocal(inEffectInterval);

            // add constraints to track the time that the cause occurred and the
            // time the effect occurred.
            builder.addLocalEquation(getTimeConstrEq(timeCauseId, causeId, timeExpr));
            builder.addAssertion(getTimeConstrStatement(timeEffectId, effectId, timeExpr));

            // add the constraint to set when we should throw an error at the
            // end of the time interval
            builder.addOutput(effectTimeIntervalEndVar);
            Expr effectOccursInInterval = eventOccursInterval(timeCauseId, effectId, pattern.effectInterval);
            Expr tendVal = new BinaryExpr(timeExpr, BinaryOp.PLUS, pattern.effectInterval.high);
            Expr preEffectTimeIntervalEnd = new UnaryExpr(UnaryOp.PRE, effectTimeIntervalEndId);
            Expr effectTimeIntervalEndHold =
                    new BinaryExpr(NEG_ONE, BinaryOp.ARROW, preEffectTimeIntervalEnd);
            Expr timeConstr = new IfThenElseExpr(causeId, tendVal, effectTimeIntervalEndHold);
            timeConstr = new IfThenElseExpr(effectOccursInInterval, NEG_ONE, timeConstr);
            timeConstr = new BinaryExpr(effectTimeIntervalEndId, BinaryOp.EQUAL, timeConstr);

            AgreeStatement timeConstrStatement = new AgreeStatement("", timeConstr, null);
            builder.addAssertion(timeConstrStatement);
            builder.addEventTime(effectTimeIntervalEndId);

            // the final property is that time is never the same as the end of
            // the effect interval
            return new BinaryExpr(timeExpr, BinaryOp.NOTEQUAL, effectTimeIntervalEndId);
        } else {
            builder.addInput(effectTimeRangeVar);
            Expr effectTimeRangeConstraint =
                    getTimeRangeConstraint(effectTimeRangeId, pattern.effectInterval);
            builder.addAssertion(new AgreeStatement(null, effectTimeRangeConstraint, null));
            // make a constraint that triggers when the event WILL happen
            builder.addAssertion(getTimeConstrStatement(timeEffectId, causeId, effectTimeRangeId));
            // register the event time
            builder.addEventTime(timeEffectId);
            // make the equation that triggers the event at the correct ime
            Expr timeEqualsPreTime = new BinaryExpr(timeExpr, BinaryOp.EQUAL, timeEffectId);
            Expr impliesEffect = new BinaryExpr(timeEqualsPreTime, BinaryOp.IMPLIES, effectId);
            return impliesEffect;
        }

    }
    
    // this method registers a timeout and creates an event that is true iff the condition
    // holds during the given interval. This is meant to essentially translate a condition
    // pattern to a purely event based pattern. it returns an IdExpr corresponding to the
    // event that triggers when the condition is held for the interval
    private static IdExpr translateCauseCondtionPattern(IdExpr causeId, AgreePatternInterval causeInterval,
            AgreeNodeBuilder builder) {
        
        AgreeVar condEventVar = new AgreeVar(CAUSE_CONDITION_EVENT_PREFIX + causeId.id, NamedType.BOOL, null, null);
        AgreeVar condEventStartVar = new AgreeVar(CAUSE_CONDITION_START_PREFIX + causeId.id, NamedType.REAL, null, null);
        AgreeVar condEventEndVar = new AgreeVar(CAUSE_CONDITION_END_PREFIX + causeId.id, NamedType.REAL, null, null);
        
        builder.addLocal(condEventVar);
        builder.addLocal(condEventStartVar);
        builder.addLocal(condEventEndVar);
        
        IdExpr condEventStartId = new IdExpr(condEventStartVar.id);
        IdExpr condEventEndId = new IdExpr(condEventEndVar.id);
        IdExpr condEventId = new IdExpr(condEventVar.id);
        
        Expr preStart = new UnaryExpr(UnaryOp.PRE, condEventStartId);
        Expr notCond = new UnaryExpr(UnaryOp.NOT, causeId);
        Expr preIsNegOne = new BinaryExpr(preStart, BinaryOp.EQUAL, NEG_ONE);
        preIsNegOne = new BinaryExpr(new BoolExpr(true), BinaryOp.ARROW, preIsNegOne);
        Expr preOrNegOne = new BinaryExpr(NEG_ONE, BinaryOp.ARROW, preStart);
        Expr causeAndNotPreReset = new BinaryExpr(causeId, BinaryOp.AND, preIsNegOne);
        //if not cause then 1 else -1 -> pre start
        Expr ifCond = new IfThenElseExpr(notCond, NEG_ONE, preOrNegOne);
        ifCond = new IfThenElseExpr(causeAndNotPreReset, timeExpr, ifCond);
        
        builder.addLocalEquation(new Equation(condEventStartId, ifCond));

        Expr plusHigh = new BinaryExpr(condEventStartId, BinaryOp.PLUS, causeInterval.high);
        Expr CondEventStartIsNeg = new BinaryExpr(condEventStartId, BinaryOp.LESS, new RealExpr(BigDecimal.ZERO));
        Expr condEventEndExpr = new IfThenElseExpr(CondEventStartIsNeg, NEG_ONE, plusHigh);
        
        builder.addLocalEquation(new Equation(condEventEndId, condEventEndExpr));
        builder.addEventTime(condEventEndId);

        Expr timeIsCondEnd = new UnaryExpr(UnaryOp.PRE, condEventEndId);
        timeIsCondEnd = new BinaryExpr(NEG_ONE, BinaryOp.ARROW, timeIsCondEnd);
        timeIsCondEnd = new BinaryExpr(timeExpr, BinaryOp.EQUAL, timeIsCondEnd);
        timeIsCondEnd = new BinaryExpr(timeIsCondEnd, BinaryOp.AND, causeId);
        builder.addLocalEquation(new Equation(condEventId, timeIsCondEnd));
        
        return condEventId;
    }

    private static Expr eventOccursInterval(IdExpr referenceTimeId, IdExpr eventId, AgreePatternInterval interval){
        Expr notNeg = new BinaryExpr(referenceTimeId, BinaryOp.GREATEREQUAL, new RealExpr(BigDecimal.ZERO));
        BinaryOp left = getIntervalLeftOp(interval);
        BinaryOp right = getIntervalRightOp(interval);
        
        Expr plusLow = new BinaryExpr(referenceTimeId, BinaryOp.PLUS, interval.low);
        Expr plusHigh = new BinaryExpr(referenceTimeId, BinaryOp.PLUS, interval.high);
        Expr low = new BinaryExpr(plusLow, left, timeExpr);
        Expr high = new BinaryExpr(timeExpr, right, plusHigh);
        Expr expr = new BinaryExpr(low, BinaryOp.AND, high);
        expr = new BinaryExpr(expr, BinaryOp.AND, eventId);
        
        return new BinaryExpr(expr, BinaryOp.AND, notNeg);
    }

    private static AgreeStatement getTimeConstrStatement(IdExpr timeId, Expr setCond, Expr setVal) {
        return new AgreeStatement("", getTimeConstrExpr(timeId, setCond, setVal), null);
    }

    private static Expr getTimeConstrExpr(IdExpr timeId, Expr setCond, Expr setVal) {
        Equation eq = getTimeConstrEq(timeId, setCond, setVal);
        return new BinaryExpr(timeId, BinaryOp.EQUAL, eq.expr);
    }

    private static Equation getTimeConstrEq(IdExpr timeId, Expr setCond, Expr setVal ) {
        Expr preTime = new UnaryExpr(UnaryOp.PRE, timeId);
        Expr elseTime = new BinaryExpr(new RealExpr(BigDecimal.valueOf(-1.0)), BinaryOp.ARROW, preTime);
        Expr timeConstr = new IfThenElseExpr(setCond, setVal, elseTime);
        return new Equation(timeId, timeConstr);
    }

    private static BinaryOp getIntervalLeftOp(AgreePatternInterval interval) {
        switch (interval.type) {
        case CLOSED:
            return BinaryOp.LESSEQUAL;
        case OPEN:
            return BinaryOp.LESS;
        case OPEN_LEFT:
            return BinaryOp.LESS;
        case OPEN_RIGHT:
            return BinaryOp.LESSEQUAL;
        default:
            throw new AgreeException("Unhandled IntervalType : " + interval.type);
        }
    }

    private static BinaryOp getIntervalRightOp(AgreePatternInterval interval) {
        switch (interval.type) {
        case CLOSED:
            return BinaryOp.LESSEQUAL;
        case OPEN:
            return BinaryOp.LESS;
        case OPEN_LEFT:
            return BinaryOp.LESSEQUAL;
        case OPEN_RIGHT:
            return BinaryOp.LESS;
        default:
            throw new AgreeException("Unhandled IntervalType : " + interval.type);
        }
    }

    private static Expr getTimeRangeConstraint(Expr timeRangeId, AgreePatternInterval interval) {
        Expr occurs = new BinaryExpr(timeRangeId, BinaryOp.MINUS, timeExpr);
        BinaryOp left = getIntervalLeftOp(interval);
        BinaryOp right = getIntervalRightOp(interval);

        Expr lower = new BinaryExpr(interval.low, left, occurs);
        Expr higher = new BinaryExpr(occurs, right, interval.high);
        return new BinaryExpr(lower, BinaryOp.AND, higher);
    }

    private static Expr getTimeRangeProperty(Expr timeRangeId, AgreePatternInterval interval) {
        BinaryOp left = getIntervalLeftOp(interval);
        BinaryOp right = getIntervalRightOp(interval);

        Expr lower = new BinaryExpr(interval.high, BinaryOp.MINUS, interval.low);
        lower = new BinaryExpr(timeExpr, BinaryOp.MINUS, lower);
        lower = new BinaryExpr(lower, left, timeRangeId);
        Expr higher = new BinaryExpr(timeRangeId, right, timeExpr);
        return new BinaryExpr(lower, BinaryOp.AND, higher);
    }

    private static Expr translatePattern(AgreeTimesPattern pattern, AgreeNodeBuilder builder) {
        throw new AgreeException("Implement this");
    }

}
