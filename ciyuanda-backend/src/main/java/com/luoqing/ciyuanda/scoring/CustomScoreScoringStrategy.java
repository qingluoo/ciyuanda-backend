package com.luoqing.ciyuanda.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.luoqing.ciyuanda.common.ErrorCode;
import com.luoqing.ciyuanda.exception.BusinessException;
import com.luoqing.ciyuanda.model.dto.question.QuestionContentDTO;
import com.luoqing.ciyuanda.model.entity.App;
import com.luoqing.ciyuanda.model.entity.Question;
import com.luoqing.ciyuanda.model.entity.ScoringResult;
import com.luoqing.ciyuanda.model.entity.UserAnswer;
import com.luoqing.ciyuanda.model.vo.QuestionVO;
import com.luoqing.ciyuanda.service.QuestionService;
import com.luoqing.ciyuanda.service.ScoringResultService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 自定义打分类应用评分策略
 */
@ScoringStrategyConfig(appType = 0, scoringStrategy = 0)
public class CustomScoreScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;

    @Resource
    private ScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        Long appId = app.getId();
        // 1. 根据 id 查询到题目和题目结果信息（按分数降序排序）
        Question question = questionService.getOne(
                Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId)
        );
        List<ScoringResult> scoringResultList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId)
                        .orderByDesc(ScoringResult::getResultScoreRange)
        );

        // 2. 统计用户的总得分
        int totalScore = 0;
        QuestionVO questionVO = QuestionVO.objToVo(question);
        List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();
        // 校验数量
        if (questionContent.size() != choices.size()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目和用户答案数量不一致");
        }
        // 遍历题目列表
        for (int i = 0; i < questionContent.size(); i++) {
            Map<String, Integer> resultMap = questionContent.get(i).getOptions().stream()
                    .collect(Collectors.toMap(QuestionContentDTO.Option::getKey, QuestionContentDTO.Option::getScore));
            Integer score = Optional.ofNullable(resultMap.get(choices.get(i))).orElse(0);
            totalScore += score;
        }

        // 3. 遍历得分结果，找到第一个用户分数大于得分范围的结果，作为最终结果
        ScoringResult maxScoringResult = scoringResultList.get(0);
        for (ScoringResult scoringResult : scoringResultList) {
            if (totalScore >= scoringResult.getResultScoreRange()) {
                maxScoringResult = scoringResult;
                break;
            }
        }

        // 4. 构造返回值，填充答案对象的属性
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoringResult.getId());
        userAnswer.setResultName(maxScoringResult.getResultName());
        userAnswer.setResultDesc(maxScoringResult.getResultDesc());
        userAnswer.setResultPicture(maxScoringResult.getResultPicture());
        userAnswer.setResultScore(totalScore);
        return userAnswer;
    }
}
