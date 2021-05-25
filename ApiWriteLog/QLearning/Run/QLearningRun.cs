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
        /// <param name="l1"></param>
        /// <param name="l2"></param>
        /// <param name="NOE"></param>
        /// <param name="numSuccess"></param>
        /// <param name="numRequest"></param>
        /// <param name="oldRewards"></param>
        /// <param name="oldQTable"></param>
        /// <param name="logState"></param>
        /// <param name="t"></param>
        /// <param name="nPull"></param>
        /// <returns></returns>
        public RWValue Run(int r, int w, int N, int l1, int l2, int NOE, int numSuccess, int numRequest, double[][] oldRewards, double[][] oldQTable,
            List<LogState> logState, int t, Dictionary<StateAndAction, int> nPull, bool violateRead, bool violateWrite)
        {
            LogState lastState = null;
            if (logState.Count > 0)
            {
                lastState = logState[^1];
            }
            var problem = new SDNProblem(oldRewards);

            var qLearning = new QLearning(_loggerQlearning, gamma: 0.8, epsilon: 0.2, alpha: 0.6,
                problem, numSuccess, numRequest, oldQTable, logState, t, nPull);

            //tính reward mới
            double newReward = Math.Round(numSuccess / (double)numRequest * 100) - 50;
            //double newReward = Math.Round(numSuccess / (double)numRequest * 100) - 50;
            if(violateRead || violateWrite)
            {
                newReward = -100;
            }

            //state khởi tạo
            int initialState = 0;
            if (lastState != null) //cập nhật lại reward tại vị trí (state,action) cũ
            {
                _loggerQlearningRun.LogInformation($"Last state {problem.GetState(lastState.l1, lastState.l2, lastState.NOE)}");
                problem.rewards[problem.GetState(lastState.l1, lastState.l2, lastState.NOE)][lastState.action] = newReward;
                //từ lần thứ 2 trở đi lấy init state bằng state trước đó
                initialState = problem.GetState(lastState.l1, lastState.l2, lastState.NOE);

                //tinh q value
                int intCurrState = problem.GetState(l1, l2, NOE);
                int intLastState = problem.GetState(lastState.l1, lastState.l2, lastState.NOE);
                qLearning.UpdateQTable(intCurrState, intLastState, lastState.action, newReward);
            }
            else // lần đầu set reward
            {
                //lựa chọn action
                int state = problem.GetState(l1, l2, NOE);

                #region khởi tạo reward chỉ dùng cho epsilon greedy
                int action = qLearning.SelectAction(state);
                problem.rewards[state][action] = newReward;
                #endregion

                #region khởi tạo q value chỉ (chỉ dùng cho ucb và softmax)
                //qLearning.InitFirstQValue(state);
                #endregion
            }

            //chèn thêm state mới vào log state
            logState.Add(new LogState
            {
                l1 = l1,
                l2 = l2,
                NOE = NOE,
                action = 0 //chưa gán action sau khi run mới có action
            });

            //show reward trước train và q value
            _loggerQlearningRun.LogInformation($"Reward:\n{problem.ShowReward()}");
            //_loggerQlearningRun.LogInformation($"QTable:\n{qLearning.ShowQTable()}");
            _loggerQlearningRun.LogInformation($"QTable sau train:\n{qLearning.ShowQTable()}");

            _loggerQlearningRun.LogInformation($"r/R: {numSuccess}/{numRequest} = {numSuccess / (double)numRequest}");

            var newAction = 0;
            try
            {
                //chạy chọn ra action từ state chỉ định
                newAction = qLearning.Run(initialState);
                if (violateRead)
                {
                    _loggerQlearningRun.LogWarning($"Violate Read, Client metric: {l2}, oldR: {r}, newR: {r - 1}");
                    newAction = 5;
                }
                if (violateWrite)
                {
                    _loggerQlearningRun.LogWarning($"Violate Write, Stale metric: {l1}, oldW: {w}, newW: {w - 1}");
                    newAction = 4;
                }
                logState[^1].action = newAction; //gán action lựa chọn là gì

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
