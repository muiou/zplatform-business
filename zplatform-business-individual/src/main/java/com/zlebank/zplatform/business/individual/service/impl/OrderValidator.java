package com.zlebank.zplatform.business.individual.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.zlebank.zplatform.business.individual.bean.Order;
import com.zlebank.zplatform.business.individual.bean.enums.OrderType;
import com.zlebank.zplatform.rmi.trade.GateWayServiceProxy;
import com.zlebank.zplatform.rmi.trade.ProdCaseServiceProxy;
import com.zlebank.zplatform.trade.analyzer.GateWayTradeAnalyzer;
import com.zlebank.zplatform.trade.bean.ResultBean;

@Service
public class OrderValidator implements IOrderValidator {
    @Autowired
    private GateWayServiceProxy gateWayService;
    @Autowired
    private ProdCaseServiceProxy prodCaseService;

    public Map<String, String> validateOrder(Order order) {
        // 验证订单数据有效性，用hibernate validator处理
        Map<String, String> resultMap = new HashMap<String, String>();
        ResultBean resultBean = GateWayTradeAnalyzer.validateOrder(order);
        if (!resultBean.isResultBool()) {
            // 订单信息长度和非空验证未通过
            resultMap.put(RET_MESSAGE, resultBean.getErrMsg());
            resultMap.put(RET_CODE, resultBean.getErrCode());
            return resultMap;
        }
        // 检验风控信息是否符合要求
        ResultBean riskResultBean = GateWayTradeAnalyzer.generateRiskBean(order
                .getRiskRateInfo());
        if (!riskResultBean.isResultBool()) {// 验证风控信息的正确性
            resultMap.put(RET_MESSAGE, riskResultBean.getErrMsg());
            resultMap.put(RET_CODE, riskResultBean.getErrCode());
            return resultMap;
        }

        // 验证订单号是否重复
        try { 
            gateWayService.verifyRepeatWebOrder(order.getOrderId(),
                    order.getTxnTime(), order.getTxnAmt(), order.getMerId(),
                    order.getMemberId());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            resultMap.put(RET_MESSAGE, e.getMessage());
            return resultMap;
        }

        // 验证商户产品版本中是否有对应的业务
        ResultBean busiResultBean = prodCaseService.verifyBusiness(order);
        if (!busiResultBean.isResultBool()) {
            resultMap.put(RET_MESSAGE, busiResultBean.getErrMsg());
            resultMap.put(RET_CODE, busiResultBean.getErrCode());
            return resultMap;
        }


        // 业务验证 
        /*if (order.getOrderType() != OrderType.CONSUME&&order.getOrderType()!=OrderType.REFUND) {//非消费类订单
            if (StringUtils.isEmpty(order.getMemberId()) || order.getMemberId().equals(Constants.ANONYMOUS_MERCH_ID)) {
                resultBean = new ResultBean("GW19", "非消费类订单不支持匿名支付");
                resultMap.put(RET_MESSAGE, resultBean.getErrMsg());
                resultMap.put(RET_CODE, resultBean.getErrCode());
                return resultMap;
            } 
        }else{//消费类订单
            if (StringUtils.isEmpty(order.getMerId())) {
                resultBean = new ResultBean("GW20", "消费类订单商户代码不能为空");
                resultMap.put(RET_MESSAGE, resultBean.getErrMsg());
                resultMap.put(RET_CODE, resultBean.getErrCode());
                return resultMap;
            }
        }*/
        if(order.getOrderType() == OrderType.CONSUME){//消费类订单
        	if (StringUtils.isEmpty(order.getMerId())) {
                resultBean = new ResultBean("GW20", "消费类订单商户代码不能为空");
                resultMap.put(RET_MESSAGE, resultBean.getErrMsg());
                resultMap.put(RET_CODE, resultBean.getErrCode());
                return resultMap;
            }
        }else if(order.getOrderType()==OrderType.RECHARGE){//充值类订单
        	if(StringUtils.isEmpty(order.getMemberId())) {
                resultBean = new ResultBean("GW20", "充值类订单会员号不能为空");
                resultMap.put(RET_MESSAGE, resultBean.getErrMsg());
                resultMap.put(RET_CODE, resultBean.getErrCode());
                return resultMap;
            }
        }else if(order.getOrderType() == OrderType.REFUND){
        	//退款订单商户号和会员号不可为空
        	if(StringUtils.isEmpty(order.getMerId())||StringUtils.isEmpty(order.getMemberId())) {
                resultBean = new ResultBean("GW20", "退款类订单商户号和会员号不能为空");
                resultMap.put(RET_MESSAGE, resultBean.getErrMsg());
                resultMap.put(RET_CODE, resultBean.getErrCode());
                return resultMap;
            }
        }else if(order.getOrderType() == OrderType.WITHDRAW){
        	if(StringUtils.isEmpty(order.getMemberId())) {
                resultBean = new ResultBean("GW20", "提现类订单会员号不能为空");
                resultMap.put(RET_MESSAGE, resultBean.getErrMsg());
                resultMap.put(RET_CODE, resultBean.getErrCode());
                return resultMap;
            }
        }
        
        
        resultMap.put(RET_MESSAGE,RET_MESSAGE_SUCCESS );
        resultMap.put(RET_CODE, RET_CODE_SUCCESS);
        return resultMap;
    }
}
