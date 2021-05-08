using Microsoft.Extensions.Logging;
using QLearningProject.Run.Models;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace QLearningProject.MachineLearning
{
    /// <summary>
    /// Cho việc lưu lại n Pull
    /// </summary>
    public class StateAndAction
    {
        public int State { get; set; }
        public int Action { get; set; }

        public override bool Equals(object obj)
        {
            if(obj.GetType() != typeof(StateAndAction))
            {
                return false;
            }

            var sa = obj as StateAndAction;
            if (State == sa.State && Action == sa.Action)
            {
                return true;
            }
            return false;
        }

        public override int GetHashCode()
        {
            return (State.ToString() + Action.ToString()).GetHashCode();
        }
    }

    public class QLearning
    {
        #region Các thuộc tính
        private static ILogger<QLearning> _loggerQlearning;

        private Random _random = new Random();
        private double _gamma;
        public double Gamma { get => _gamma; }
        private double _epsilon;
        public double Epsilon { get => _epsilon; }
        private double _alpha;
        public double Alpha { get => _alpha; }
        private double _c = 0.05;

        private double[][] _qTable;
        public double[][] QTable { get => _qTable; }

        public Dictionary<StateAndAction, int> _nPull;
        public int _t { get; set; }

        private double T = 0.1;


        private IQLearningProblem _qLearningProblem;
        private LogState[] _logState;

        private int numSuccessForAction;
        private int numRequestForAction;
        #endregion

        /// <summary>
        /// Chạy q learning với đầu vào là qtable cũ để tính qtable mới
        /// </summary>
        /// <param name="loggerQlearning"></param>
        /// <param name="gamma"></param>
        /// <param name="epsilon"></param>
        /// <param name="alpha"></param>
        /// <param name="qLearningProblem"></param>
        /// <param name="numSuccessForAction"></param>
        /// <param name="numRequestForAction"></param>
        /// <param name="oldQTable"></param>
        /// <param name="logState"></param>
        /// <param name="t"></param>
        /// <param name="nPull"></param>
        public QLearning(ILogger<QLearning> loggerQlearning, double gamma, double epsilon, double alpha, IQLearningProblem qLearningProblem,
            int numSuccessForAction, int numRequestForAction,
            double[][] oldQTable, LogState[] logState, int t, Dictionary<StateAndAction, int> nPull)
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

            if (t <= 0)
            {
                _t = 1;
            }
            else
            {
                _t = t;
            }

            if (nPull == null)
            {
                _nPull = new Dictionary<StateAndAction, int>();
            }
            else
            {
                _nPull = nPull;
            }
            this.numSuccessForAction = numSuccessForAction;
            this.numRequestForAction = numRequestForAction;
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
            //return UCBSelectAction(_t, initialState);
            //return SoftMaxSelectAction(initialState);
        }

        /// <summary>
        /// Lựa chọn action theo epsilon greedy
        /// theo q value max hoặc random theo r/R
        /// </summary>
        /// <param name="currentState"></param>
        /// <returns></returns>
        public int SelectAction(int currentState)
        {
            double n = _random.NextDouble();
            int action;
            if (n < _epsilon)
            {
                var div = numSuccessForAction / (double)numRequestForAction;
                if (div <= 0.5)
                {
                    action = _random.Next(0, 2);
                }
                else if (div > 0.5 && div < 1)
                {
                    action = _random.Next(2, 4);
                }
                else //== 1
                {
                    action = _random.Next(4, 6);
                }
            }
            else
            {
                var qValueMax = _qTable[currentState].Max();
                action = _qTable[currentState].ToList().IndexOf(qValueMax);
            }

            _t++; //đếm số lần pull
            return action;
        }

        /// <summary>
        /// Lựa chọn action theo UCB
        /// </summary>
        /// <param name="t"></param>
        /// <param name="currentState"></param>
        /// <returns></returns>
        public int UCBSelectAction(int t, int currentState)
        {
            var bestAction = 0;
            var sa = new StateAndAction { State = currentState, Action = 0 };
            if (!_nPull.ContainsKey(sa))
            {
                _nPull.Add(sa, 1);
            }
            var bestUCB = _qTable[currentState][0] + _c * Math.Sqrt(Math.Log(t) / _nPull[sa]);
            for (int i = 1; i < 6; i++)
            {
                var saTemp = new StateAndAction { State = currentState, Action = i };
                if (!_nPull.ContainsKey(sa))
                {
                    _nPull.Add(sa, 1);
                }
                var ucb = _qTable[currentState][i] + _c * Math.Sqrt(Math.Log(t) / _nPull[sa]);
                if (ucb > bestUCB)
                {
                    sa = saTemp;
                    bestUCB = ucb;
                    bestAction = i;
                }
            }
            _nPull[sa]++; //tăng pull lên
            return bestAction;
        }

        /// <summary>
        /// Lựa chọn action theo softmax
        /// </summary>
        /// <param name="currentState"></param>
        /// <returns></returns>
        public int SoftMaxSelectAction(int currentState)
        {
            List<double> ListP = new List<double>() { 0, 0, 0, 0, 0, 0 };
            double sum = 0;
            for (int i = 0; i < 6; i++)
            {
                sum += Math.Pow(Math.E, _qTable[currentState][i]) / T;
            }

            for (int i = 0; i < 6; i++)
            {
                ListP[i] = Math.Pow(Math.E, _qTable[currentState][i]) / T / sum;
            }

            List<double> SumP = new List<double>() { 0, 0, 0, 0, 0, 0 };
            for (int i = 0; i < 6; i++)
            {
                double subSum = 0;
                for(int j = 0; j < i; j++)
                {
                    subSum += ListP[j];
                }
                SumP[i] = subSum;
            }

            var randomChoose = _random.NextDouble();
            for (int i = 0; i < 6; i++)
            {
                if (randomChoose >= SumP[i])
                {
                    //chưa có state action này thì khởi tạo
                    var sa = new StateAndAction { State = currentState, Action = i };
                    if (!_nPull.ContainsKey(sa))
                    {
                        _nPull.Add(sa, 1);
                    }
                    return i;
                }
            }
            return 0;
        }

        /// <summary>
        /// Khởi tạo giá trị q value dùng cho softmax và ucb
        /// </summary>
        /// <param name="initState"></param>
        public void InitFirstQValue(int initState)
        {
            _qTable[initState][0] = 0;
            _qTable[initState][1] = 0;
            _qTable[initState][2] = 0;
            _qTable[initState][3] = 0;
            _qTable[initState][4] = 0;
            _qTable[initState][5] = 0;
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
            #region select action dựa theo q value max hoặc theo r/R
            int action = SelectAction(currentState);
            //int action = UCBSelectAction(_t, currentState);
            //int action = SoftMaxSelectAction(currentState);
            #endregion

            #region lấy reward
            //lấy ra giá trị reward tại s và a chỉ định
            double saReward = _qLearningProblem.GetReward(currentState, action);
            #endregion

            #region tính q value
            //lấy max đã có tại action đang xét
            double maxQValue = _qTable[currentState].Max();
            //tính ra value mới
            double qCurrentState = _qTable[currentState][action] + _alpha * (saReward + _gamma * maxQValue - _qTable[currentState][action]);
            //cập nhật vào q table tại s(curr) a(random)
            _qTable[currentState][action] = qCurrentState;
            #endregion

            #region tính q value theo ucb & soft max
            //refer: https://github.com/SahanaRamnath/MultiArmedBandit_RL/tree/master/UCB
            //tính q value cho ucb
            //var sa = new StateAndAction { State = currentState, Action = action };
            //double qCurrentStateNew = _qTable[currentState][action] + (saReward - _qTable[currentState][action]) / _nPull[sa];
            //_qTable[currentState][action] = qCurrentStateNew;
            #endregion
        }

        /// <summary>
        /// Random ra state để train
        /// </summary>
        /// <param name="numberOfStates">Số state</param>
        /// <returns></returns>
        private int RandomInitialState()
        {
            if (_logState.Length > 0)
            {
                int index = _random.Next(0, _logState.Length);
                var state = _logState[index];
                return _qLearningProblem.GetState(state.l1, state.l2, state.NOE);
            }
            return _random.Next(0, _qLearningProblem.NumberOfStates);
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
