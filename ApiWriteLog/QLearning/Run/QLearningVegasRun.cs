using Microsoft.Extensions.Logging;
using QLearningProject.Common;
using QLearningProject.MachineLearning;
using QLearningProject.Problems;
using QLearningProject.Run.Models;
using System;
using System.Collections.Generic;

namespace QLearningProject.Run
{
    public class QLearningVegasRun
    {
        private static ILogger<QLearningVegasRun> _loggerQlearningRun;
        private static ILogger<QLearningVegas> _loggerQlearning;
        public QLearningVegasRun(ILogger<QLearningVegasRun> logger, ILogger<QLearningVegas> loggerQlearning)
        {
            _loggerQlearningRun = logger;
            _loggerQlearning = loggerQlearning;
        }

        /// <summary>
        /// Chạy xong trả ra R W mới
        /// </summary>
        /// <returns></returns>
        public RWValueVegas Run(int r, int w, int N, int l1, int l2, int NOE, int numSuccess, int numRequest, double[][] oldRewards, double[][] oldQTable,
            List<LogState> logState, Queue<double> logCSC, bool violateRead, bool violateWrite)
        {
            LogState lastState = null;
            if (logState.Count > 0)
            {
                lastState = logState[^1];
            }
            var problem = new SDNProblem(oldRewards);

            var qLearning = new QLearningVegas(_loggerQlearning, gamma: 0.8, epsilon: 0.1, alpha: 0.6,
                problem, numSuccess, numRequest, oldQTable, logState, logCSC, N);

            _loggerQlearningRun.LogInformation($"r/R: {numSuccess}/{numRequest} = {numSuccess / (double)numRequest}");

            //tính reward mới
            double newReward = Math.Round(numSuccess / (double)numRequest * 100);
            if (violateRead || violateWrite)
            {
                newReward = -100;
            }
            //state khởi tạo khi training xong
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

                //khoi tao reward
                int action = qLearning.SelectAction(state);
                problem.rewards[state][action] = newReward;
            }

            //chèn thêm state mới vào log state
            logState.Add(new LogState
            {
                l1 = l1,
                l2 = l2,
                NOE = NOE,
                action = 0 //chưa gán action sau khi run mới có action
            });

            //show reward và q value
            _loggerQlearningRun.LogInformation($"Reward:\n{problem.ShowReward()}");
            //_loggerQlearningRun.LogInformation($"QTable:\n{qLearning.ShowQTable()}");
            _loggerQlearningRun.LogInformation($"QTable sau train:\n{qLearning.ShowQTable()}");

            var newAction = 0;
            try
            {
                //chạy chọn ra action từ state chỉ định
                newAction = qLearning.Run(initialState);
                if (violateRead)
                {
                    newAction = 5;
                }
                if (violateWrite)
                {
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
            RWValueVegas rwValue = new RWValueVegas()
            {
                R = r,
                W = w,
                Action = newAction,
                rewards = problem.rewards,
                qTable = qLearning.QTable
            };
            return rwValue;
        }
    }
}
