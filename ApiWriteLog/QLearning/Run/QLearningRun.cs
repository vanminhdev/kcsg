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

            var qLearning = new QLearning(_loggerQlearning, gamma: 0.8, epsilon: 0.4, alpha: 0.6,
                problem, numSuccessForAction, numRequestForAction, oldQTable, logState, t, nPull);

            _loggerQlearningRun.LogInformation($"r/R: {numSuccessForAction}/{numRequestForAction} = {numSuccessForAction/(double)numRequestForAction}");

            //tính reward mới
            double newReward = Math.Round((newNumSuccess - oldNumSuccess) / (double)(newNumRequest - oldNumRequest) * 100);

            //state khởi tạo
            int initialState = 0;
            if (lastState != null) //cập nhật lại reward tại vị trí (state,action) cũ
            {
                _loggerQlearningRun.LogInformation($"Last state {problem.GetState(lastState.l1, lastState.l2, lastState.NOE)}");

                problem.rewards[problem.GetState(lastState.l1, lastState.l2, lastState.NOE)][lastState.action] = newReward;
                //từ lần thứ 2 trở đi lấy init state bằng state trước đó
                initialState = problem.GetState(lastState.l1, lastState.l2, lastState.NOE);
            }
            else // lần đầu set reward
            {
                //lựa chọn action
                int state = problem.GetState(l1, l2, NOE);

                #region khởi tạo reward chỉ dùng cho epsilon greedy
                //int action = qLearning.SelectAction(state);
                //problem.rewards[state][action] = newReward;
                #endregion

                #region khởi tạo q value chỉ (chỉ dùng cho ucb và softmax)
                //qLearning.InitFirstQValue(state);
                //L1 L2 NOE
                #endregion
            }

            //show reward trước train và q value
            _loggerQlearningRun.LogInformation($"Reward:\n{problem.ShowReward()}");
            //_loggerQlearningRun.LogInformation($"QTable:\n{qLearning.ShowQTable()}");
            qLearning.TrainAgent(200);
            _loggerQlearningRun.LogInformation($"QTable sau train:\n{qLearning.ShowQTable()}");

            var newAction = 0;
            try
            {
                //chạy chọn ra action từ state chỉ định
                newAction = qLearning.Run(initialState);
                _loggerQlearningRun.LogInformation($"from state {initialState} new action: {newAction}");
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
                    newR = 1;
                }

                if (newW < 1)
                {
                    newW = 1;
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
