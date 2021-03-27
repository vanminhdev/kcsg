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
        private static ILogger<QLearningRun> _logger;
        public QLearningRun(ILogger<QLearningRun> logger)
        {
            _logger = logger;
        }

        public RWValue Run(int r, int w, int N, int oldNumSuccess, int oldNumRequest, int newNumSuccess, int newNumRequest,
            int l1, int l2, int NOE, int numSuccessForAction, int numRequestForAction, double[][] oldRewards, double[][] oldQTable,
            LogState[] logState)
        {
            LogState lastState = null;
            if (logState.Length > 0)
            {
                lastState = logState[^1];
            }
            var problem = new SDNProblem(numSuccessForAction, numRequestForAction, l1, l2, NOE, oldRewards);
            var qLearning = new QLearning(0.8, 0.5, 0.6, problem, numSuccessForAction, numRequestForAction, oldQTable, logState);

            //update reward
            var newReward = Math.Round((newNumSuccess - oldNumSuccess) / (double)(newNumRequest - oldNumRequest) * 100);
            //var newReward = (new Random()).Next(80, 100); //test
            if (lastState != null)
            {
                problem.rewards[problem.GetState(lastState.l1, lastState.l2, lastState.NOE)][lastState.action] = newReward;
            }
            else // lần đầu set reward
            {
                int state = problem.GetState(l1, l2, NOE);
                //int action = qLearning.SelectAction(state);
                int action = qLearning.UCBSelectAction(qLearning.T, state);
                problem.rewards[state][action] = newReward;
            }

            _logger.LogInformation($"Last state {lastState}");

            //show reward và q value
            _logger.LogInformation($"Reward:\n{problem.ShowReward()}");
            _logger.LogInformation($"QTable:\n{qLearning.ShowQTable()}");

            qLearning.TrainAgent(2000);
            _logger.LogInformation($"QTable sau train:\n{qLearning.ShowQTable()}");
            int initialState = problem.GetState(l1, l2, NOE);
            var newAction = 0;
            try
            {
                newAction = qLearning.Run(initialState);
                _logger.LogInformation($"new action: {newAction}");
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
                _logger.LogError($"Qlearning Error: {ex.Message}");
            }
            RWValue rwValue = new RWValue()
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
