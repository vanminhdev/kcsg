using Microsoft.Extensions.Logging;
using QLearningProject.Run.Models;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace QLearningProject.MachineLearning
{
    public class QLearningVegas
    {
        #region Các thuộc tính
        private static ILogger<QLearningVegas> _loggerQlearning;

        private Random _random = new Random();
        private double _gamma;
        public double Gamma { get => _gamma; }
        private double _epsilon;
        public double Epsilon { get => _epsilon; }
        private double _alpha;
        public double Alpha { get => _alpha; }

        private double[][] _qTable;
        public double[][] QTable { get => _qTable; }

        private IQLearningProblem _qLearningProblem;
        private List<LogState> _logState;

        private Queue<double> _logCSC;

        private int _numCtrl;

        private int _numSuccessForAction;
        private int _numRequestForAction;
        #endregion

        /// <summary>
        /// Chạy q learning với đầu vào là qtable cũ để tính qtable mới
        /// </summary>
        public QLearningVegas(ILogger<QLearningVegas> loggerQlearning, double gamma, double epsilon, double alpha, IQLearningProblem qLearningProblem,
            int numSuccessForAction, int numRequestForAction,
            double[][] oldQTable, List<LogState> logState, Queue<double> logCSC, int numCtrl)
        {
            _loggerQlearning = loggerQlearning;
            _qLearningProblem = qLearningProblem;
            if (oldQTable == null)
            {
                _qTable = new double[_qLearningProblem.NumberOfStates][];
                for (int i = 0; i < _qLearningProblem.NumberOfStates; i++)
                {
                    _qTable[i] = new double[] { 0, 0, 0, 0, 0, 0 };
                }
            }
            else
            {
                _qTable = oldQTable;
            }
            _gamma = gamma;
            _epsilon = epsilon;
            _alpha = alpha;
            _logState = logState;
            _numSuccessForAction = numSuccessForAction;
            _numRequestForAction = numRequestForAction;

            _logCSC = logCSC;
            _numCtrl = numCtrl;
        }

        /// <summary>
        /// Bắt đầu training bằng cách khơi tạo init sate bằng cách random state trong tập state đã biết
        /// sau đó tính q value
        /// </summary>
        /// <param name="numberOfIterations"></param>
        public void TrainAgent(int numberOfIterations)
        {
            for (int i = 0; i < numberOfIterations; i++)
            {
                //lấy init state là một random trong các state đã từng có trong quá khứ
                int initialState = RandomInitialState();
                //tính value cho q table
                InitializeEpisode(initialState);
            }
        }

        /// <summary>
        /// Chạy sau khi train xong trả ra action mới, với mỗi action mới sẽ cho ra sự thay đổi R W tương ứng
        /// </summary>
        /// <param name="initialState"></param>
        /// <returns></returns>
        public int Run(int initialState)
        {
            if (initialState < 0 || initialState > _qLearningProblem.NumberOfStates) 
                throw new ArgumentException($"The initial state can be between [0-{_qLearningProblem.NumberOfStates}", nameof(initialState));
            return SelectAction(initialState);
        }

        /// <summary>
        /// Lựa chọn action vegas
        /// </summary>
        /// <param name="currentState"></param>
        /// <returns></returns>
        public int SelectAction(int currentState)
        {
            double n = _random.NextDouble();
            int action;
            if (n < _epsilon)
            {
                double CSC = _numSuccessForAction / (double)_numRequestForAction;
                _logCSC.Enqueue(CSC);
                double CSCBase = _logCSC.Max();
                if (CSCBase == 0)
                    CSCBase = 1;

                if (_logCSC.Count > 10)
                {
                    _logCSC.Dequeue();
                }

                var diff = _numCtrl * ((1 - CSC) / CSCBase);
                if (diff <= 8)
                {
                    action = _random.Next(4, 6);
                }
                else if (diff > 8 && diff < 11)
                {
                    action = _random.Next(2, 4);
                }
                else //
                {
                    action = _random.Next(0, 2);
                }
            }
            else
            {
                var qValueMax = _qTable[currentState].Max();
                action = _qTable[currentState].ToList().IndexOf(qValueMax);
            }
            return action;
        }

        /// <summary>
        /// Tính value cho q table
        /// </summary>
        /// <param name="initialState">trạng thái khởi tạo</param>
        private void InitializeEpisode(int initialState)
        {
            int currentState = initialState;
            for (int i = 0; i < 6; i++)
            {
                TakeAction(currentState);
            }
        }

        /// <summary>
        /// Lấy ra action theo quy tắc và tính q value
        /// </summary>
        /// <param name="currentState">trạng thái đang xét</param>
        private void TakeAction(int currentState)
        {
            //select action dựa theo q value max hoặc theo r/R
            int action = SelectAction(currentState);

            //lấy ra giá trị reward tại s và a chỉ định
            double saReward = _qLearningProblem.GetReward(currentState, action);

            //lấy max đã có tại action đang xét
            double maxQValue = _qTable[currentState].Max();

            //tính ra value mới
            double qCurrentState = _qTable[currentState][action] + _alpha * (saReward + _gamma * maxQValue - _qTable[currentState][action]);

            //cập nhật vào q table tại s(curr) a(random)
            _qTable[currentState][action] = qCurrentState;
        }

        /// <summary>
        /// Random ra state để train, danh sách state là những state đã từng xảy ra trong quá khứ + 1 state vừa xảy ra
        /// </summary>
        /// <param name="numberOfStates">Số state</param>
        /// <returns></returns>
        private int RandomInitialState()
        {
            if (_logState.Count > 0)
            {
                int index = _random.Next(0, _logState.Count);
                var state = _logState[index];
                return _qLearningProblem.GetState(state.l1, state.l2, state.NOE);
            }
            return 0;
        }

        /// <summary>
        /// Show Q table
        /// </summary>
        /// <returns></returns>
        public string ShowQTable()
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < _qLearningProblem.NumberOfStates; i++)
            {
                for (int j = 0; j < _qLearningProblem.NumberOfActions; j++)
                {
                    if (_qTable[i][j] != 0)
                    {
                        sb.Append($"{i}: {_qTable[i][0]} {_qTable[i][1]} {_qTable[i][2]} {_qTable[i][3]} {_qTable[i][4]} {_qTable[i][5]}\n");
                        break;
                    }
                }
            }
            return sb.ToString();
        }
    }
}
