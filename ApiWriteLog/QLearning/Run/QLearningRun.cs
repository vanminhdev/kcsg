using Microsoft.Extensions.Logging;
using QLearningProject.Common;
using QLearningProject.MachineLearning;
using QLearningProject.Problems;
using QLearningProject.Run.Models;
using System;
using System.Collections.Generic;

namespace QLearningProject.Run
{
    public class QLearningRun
    {
        private static ILogger<QLearningRun> _loggerQlearningRun;
        private static ILogger<QLearning> _loggerQlearning;
        public QLearningRun(ILogger<QLearningRun> logger, ILogger<QLearning> loggerQlearning)
        {
            _loggerQlearningRun = logger;
            _loggerQlearning = loggerQlearning;
        }

        /// <summary>
        /// Chạy xong trả ra R W mới
        /// </summary>
        /// <param name="r"></param>
        /// <param name="w"></param>
        /// <param name="N"></param>
        /// <param name="oldNumSuccess"></param>
        /// <param name="oldNumRequest"></param>
        /// <param name="newNumSuccess"></param>
        /// <param name="newNumRequest"></param>
        /// <param name="l1"></param>
        /// <param name="l2"></param>
        /// <param name="NOE"></param>
        /// <param name="numSuccessForAction"></param>
        /// <param name="numRequestForAction"></param>
        /// <param name="oldRewards"></param>
        /// <param name="oldQTable"></param>
        /// <param name="logState"></param>
        /// <param name="t"></param>
        /// <param name="nPull"></param>
        /// <returns></returns>
        public RWValue Run(int r, int w, int N, int oldNumSuccess, int oldNumRequest, int newNumSuccess, int newNumRequest,
            int l1, int l2, int NOE, int numSuccessForAction, int numRequestForAction, double[][] oldRewards, double[][] oldQTable,
            LogState[] logState, int t, Dictionary<StateAndAction, int> nPull)
        {
            LogState lastState = null;
            if (logState.Length > 0)
            {
                lastState = logState[^1];
            }
            var problem = new SDNProblem(oldRewards);
            var qLearning = new QLearning(_loggerQlearning, 0.8, 0.5, 0.6, problem, numSuccessForAction, numRequestForAction, oldQTable, logState, t, nPull);

            //tính reward mới
            var newReward = Math.Round((newNumSuccess - oldNumSuccess) / (double)(newNumRequest - oldNumRequest) * 100);

            if (lastState != null) //cập nhật lại reward tại vị trí (state,action) cũ
            {
                problem.rewards[problem.GetState(lastState.l1, lastState.l2, lastState.NOE)][lastState.action] = newReward;
            }
            else // lần đầu set reward
            {
                //lựa chọn action
                int state = problem.GetState(l1, l2, NOE);
                int action = qLearning.SelectAction(state);
                //int action = qLearning.UCBSelectAction(qLearning._t, state);
                problem.rewards[state][action] = newReward;
            }

            _loggerQlearningRun.LogInformation($"Last state {lastState}");

            //show reward và q value
            _loggerQlearningRun.LogInformation($"Reward:\n{problem.ShowReward()}");
            _loggerQlearningRun.LogInformation($"QTable:\n{qLearning.ShowQTable()}");

            qLearning.TrainAgent(2000);
            _loggerQlearningRun.LogInformation($"QTable sau train:\n{qLearning.ShowQTable()}");
            int initialState = problem.GetState(l1, l2, NOE);
            var newAction = 0;
            try
            {
                newAction = qLearning.Run(initialState);
                _loggerQlearningRun.LogInformation($"new action: {newAction}");
                int newR = r;
                int newW = w;
                if (newAction == 0)
                {
                    newW = w + 2;
                }
                else if (newAction == 1)
                {
                    newR = r + 2;
                }
                else if (newAction == 2)
                {
                    newW = w + 1;
                }
                else if (newAction == 3)
                {
                    newR = r + 1;
                }
                else if (newAction == 4)
                {
                    newW = w - 1;
                }
                else if (newAction == 5)
                {
                    newR = r - 1;
                }

                if (newR < 1)
                {
                    r = 1;
                }

                if (newW < 0)
                {
                    w = 0;
                }

                if (newR + newW <= N) //nếu vẫn thoả thì thay đổi
                {
                    r = newR;
                    w = newW;
                }
            }
            catch (Exception ex)
            {
                _loggerQlearningRun.LogError($"Qlearning Error: {ex.Message}");
            }
            RWValue rwValue = new RWValue()
            {
                R = r,
                W = w,
                Action = newAction,
                rewards = problem.rewards,
                qTable = qLearning.QTable,
                t = qLearning._t,
                nPull = qLearning._nPull
            };
            return rwValue;
        }
    }
}
