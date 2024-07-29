let timerInterval;
let seconds = 0;
let isRunning = false;

const display = document.getElementById('display');
const startBtn = document.getElementById('startBtn');
const resetBtn = document.getElementById('resetBtn');

function formatTime(seconds) {
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${String(hrs).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function updateDisplay() {
    display.textContent = formatTime(seconds);
}

function startTimer() {
    if (isRunning) return;
    isRunning = true;
    timerInterval = setInterval(() => {
        seconds++;
        updateDisplay();
    }, 1000);
    startBtn.disabled = true;
    resetBtn.disabled = false;
}

function resetTimer() {
    clearInterval(timerInterval);
    isRunning = false;
    seconds = 0;
    updateDisplay();
    startBtn.disabled = false;
    resetBtn.disabled = true;
}

startBtn.addEventListener('click', startTimer);
resetBtn.addEventListener('click', resetTimer);


resetBtn.disabled = true;
