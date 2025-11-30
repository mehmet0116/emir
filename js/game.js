/**
 * 🍬 Şeker Patlatma Oyunu - Ana JavaScript Dosyası
 * Profesyonel seviye sistemi ve akıcı oyun mekaniği
 */

// ==========================================
// OYUN AYARLARI VE SABİTLER
// ==========================================
const GAME_CONFIG = {
    boardSize: 8,
    candyTypes: ['red', 'blue', 'green', 'yellow', 'purple', 'orange'],
    candyEmojis: {
        red: '🍎',
        blue: '💎',
        green: '🍀',
        yellow: '⭐',
        purple: '🍇',
        orange: '🍊'
    },
    baseScore: 10,
    comboMultiplier: 1.5,
    matchBonus: {
        4: 50,  // 4'lü eşleşme bonusu
        5: 100, // 5'li eşleşme bonusu
        6: 200  // 6'lı ve üzeri eşleşme bonusu
    },
    totalLevels: 50,
    animationDuration: 300
};

// Seviye Konfigürasyonları
const LEVELS = [];
for (let i = 1; i <= GAME_CONFIG.totalLevels; i++) {
    LEVELS.push({
        level: i,
        targetScore: 500 + (i * 300) + Math.floor(i / 5) * 500,
        moves: Math.max(15, 35 - Math.floor(i / 3)),
        starThresholds: {
            1: 500 + (i * 300),
            2: 700 + (i * 400),
            3: 1000 + (i * 500)
        }
    });
}

// ==========================================
// OYUN DURUMU
// ==========================================
let gameState = {
    currentLevel: 1,
    score: 0,
    moves: 30,
    targetScore: 1000,
    stars: 0,
    totalStars: 0,
    maxUnlockedLevel: 1,
    levelStars: {}, // Her seviyede kazanılan yıldızlar
    board: [],
    selectedCandy: null,
    isAnimating: false,
    comboCount: 0,
    settings: {
        sound: true,
        music: true,
        animations: true
    }
};

// ==========================================
// DOM ELEMENTLERİ
// ==========================================
const elements = {
    // Ekranlar
    mainMenu: document.getElementById('main-menu'),
    levelSelect: document.getElementById('level-select'),
    gameScreen: document.getElementById('game-screen'),
    levelComplete: document.getElementById('level-complete'),
    levelFailed: document.getElementById('level-failed'),
    settingsScreen: document.getElementById('settings-screen'),
    newLevelUnlock: document.getElementById('new-level-unlock'),
    
    // Oyun Elemanları
    gameBoard: document.getElementById('game-board'),
    currentLevel: document.getElementById('current-level'),
    movesLeft: document.getElementById('moves-left'),
    currentScore: document.getElementById('current-score'),
    targetScore: document.getElementById('target-score'),
    scoreProgress: document.getElementById('score-progress'),
    levelsGrid: document.getElementById('levels-grid'),
    
    // İstatistikler
    totalStars: document.getElementById('total-stars'),
    currentLevelDisplay: document.getElementById('current-level-display'),
    levelSelectStars: document.getElementById('level-select-stars'),
    
    // Popup Elemanları
    finalScore: document.getElementById('final-score'),
    failedScore: document.getElementById('failed-score'),
    bonusSection: document.getElementById('bonus-section'),
    bonusAmount: document.getElementById('bonus-amount'),
    unlockedLevel: document.getElementById('unlocked-level'),
    
    // Yıldızlar
    star1: document.getElementById('star-1'),
    star2: document.getElementById('star-2'),
    star3: document.getElementById('star-3'),
    
    // Butonlar
    playBtn: document.getElementById('play-btn'),
    levelsBtn: document.getElementById('levels-btn'),
    settingsBtn: document.getElementById('settings-btn'),
    backToMenu: document.getElementById('back-to-menu'),
    backFromGame: document.getElementById('back-from-game'),
    hintBtn: document.getElementById('hint-btn'),
    shuffleBtn: document.getElementById('shuffle-btn'),
    nextLevelBtn: document.getElementById('next-level-btn'),
    replayBtn: document.getElementById('replay-btn'),
    menuBtn: document.getElementById('menu-btn'),
    retryBtn: document.getElementById('retry-btn'),
    failedMenuBtn: document.getElementById('failed-menu-btn'),
    closeSettings: document.getElementById('close-settings'),
    continueBtn: document.getElementById('continue-btn'),
    
    // Ayarlar
    soundToggle: document.getElementById('sound-toggle'),
    musicToggle: document.getElementById('music-toggle'),
    animationToggle: document.getElementById('animation-toggle')
};

// ==========================================
// OYUN İNİSİALİZASYONU
// ==========================================
function init() {
    loadGameData();
    setupEventListeners();
    createFloatingCandies();
    updateUI();
}

function loadGameData() {
    const savedData = localStorage.getItem('candyCrushData');
    if (savedData) {
        const data = JSON.parse(savedData);
        gameState.totalStars = data.totalStars || 0;
        gameState.maxUnlockedLevel = data.maxUnlockedLevel || 1;
        gameState.levelStars = data.levelStars || {};
        gameState.settings = data.settings || gameState.settings;
    }
}

function saveGameData() {
    const data = {
        totalStars: gameState.totalStars,
        maxUnlockedLevel: gameState.maxUnlockedLevel,
        levelStars: gameState.levelStars,
        settings: gameState.settings
    };
    localStorage.setItem('candyCrushData', JSON.stringify(data));
}

function updateUI() {
    elements.totalStars.textContent = gameState.totalStars;
    elements.currentLevelDisplay.textContent = gameState.maxUnlockedLevel;
    elements.levelSelectStars.textContent = gameState.totalStars;
    
    // Ayarları güncelle
    elements.soundToggle.checked = gameState.settings.sound;
    elements.musicToggle.checked = gameState.settings.music;
    elements.animationToggle.checked = gameState.settings.animations;
}

// ==========================================
// EVENT LISTENERS
// ==========================================
function setupEventListeners() {
    // Ana Menü
    elements.playBtn.addEventListener('click', () => startLevel(gameState.maxUnlockedLevel));
    elements.levelsBtn.addEventListener('click', showLevelSelect);
    elements.settingsBtn.addEventListener('click', showSettings);
    
    // Navigasyon
    elements.backToMenu.addEventListener('click', showMainMenu);
    elements.backFromGame.addEventListener('click', confirmExit);
    
    // Oyun Kontrolleri
    elements.hintBtn.addEventListener('click', showHint);
    elements.shuffleBtn.addEventListener('click', shuffleBoard);
    
    // Popup Butonları
    elements.nextLevelBtn.addEventListener('click', nextLevel);
    elements.replayBtn.addEventListener('click', () => startLevel(gameState.currentLevel));
    elements.menuBtn.addEventListener('click', showMainMenu);
    elements.retryBtn.addEventListener('click', () => startLevel(gameState.currentLevel));
    elements.failedMenuBtn.addEventListener('click', showMainMenu);
    elements.closeSettings.addEventListener('click', hideSettings);
    elements.continueBtn.addEventListener('click', hideNewLevelUnlock);
    
    // Ayarlar
    elements.soundToggle.addEventListener('change', (e) => {
        gameState.settings.sound = e.target.checked;
        saveGameData();
    });
    elements.musicToggle.addEventListener('change', (e) => {
        gameState.settings.music = e.target.checked;
        saveGameData();
    });
    elements.animationToggle.addEventListener('change', (e) => {
        gameState.settings.animations = e.target.checked;
        saveGameData();
    });
}

// ==========================================
// EKRAN GEÇİŞLERİ
// ==========================================
function showScreen(screen) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    screen.classList.add('active');
}

function showMainMenu() {
    showScreen(elements.mainMenu);
    updateUI();
}

function showLevelSelect() {
    generateLevelButtons();
    showScreen(elements.levelSelect);
}

function showSettings() {
    elements.settingsScreen.classList.add('active');
}

function hideSettings() {
    elements.settingsScreen.classList.remove('active');
}

function showNewLevelUnlock(level) {
    elements.unlockedLevel.textContent = level;
    elements.newLevelUnlock.classList.add('active');
}

function hideNewLevelUnlock() {
    elements.newLevelUnlock.classList.remove('active');
}

function confirmExit() {
    if (confirm('Oyundan çıkmak istediğinize emin misiniz? İlerlemeniz kaydedilmeyecek.')) {
        showMainMenu();
    }
}

// ==========================================
// SEVİYE SİSTEMİ
// ==========================================
function generateLevelButtons() {
    elements.levelsGrid.innerHTML = '';
    
    for (let i = 1; i <= GAME_CONFIG.totalLevels; i++) {
        const button = document.createElement('button');
        button.className = 'level-btn';
        
        const isUnlocked = i <= gameState.maxUnlockedLevel;
        const stars = gameState.levelStars[i] || 0;
        
        if (isUnlocked) {
            button.classList.add(stars > 0 ? 'completed' : 'unlocked');
            button.innerHTML = `
                <span class="level-number">${i}</span>
                <div class="level-stars">
                    <span class="star ${stars >= 1 ? 'earned' : ''}">⭐</span>
                    <span class="star ${stars >= 2 ? 'earned' : ''}">⭐</span>
                    <span class="star ${stars >= 3 ? 'earned' : ''}">⭐</span>
                </div>
            `;
            button.addEventListener('click', () => startLevel(i));
        } else {
            button.classList.add('locked');
        }
        
        elements.levelsGrid.appendChild(button);
    }
}

function startLevel(level) {
    const levelConfig = LEVELS[level - 1];
    
    gameState.currentLevel = level;
    gameState.score = 0;
    gameState.moves = levelConfig.moves;
    gameState.targetScore = levelConfig.targetScore;
    gameState.stars = 0;
    gameState.selectedCandy = null;
    gameState.isAnimating = false;
    gameState.comboCount = 0;
    
    // UI güncelle
    elements.currentLevel.textContent = level;
    elements.movesLeft.textContent = gameState.moves;
    elements.currentScore.textContent = gameState.score;
    elements.targetScore.textContent = gameState.targetScore;
    updateProgressBar();
    
    // Tahtayı oluştur
    createBoard();
    
    // Ekranı göster
    showScreen(elements.gameScreen);
}

function nextLevel() {
    elements.levelComplete.classList.remove('active');
    
    if (gameState.currentLevel < GAME_CONFIG.totalLevels) {
        startLevel(gameState.currentLevel + 1);
    } else {
        showMainMenu();
        alert('🎉 Tebrikler! Tüm seviyeleri tamamladınız!');
    }
}

// ==========================================
// OYUN TAHTASI
// ==========================================
function createBoard() {
    gameState.board = [];
    elements.gameBoard.innerHTML = '';
    
    for (let row = 0; row < GAME_CONFIG.boardSize; row++) {
        gameState.board[row] = [];
        for (let col = 0; col < GAME_CONFIG.boardSize; col++) {
            const candy = createCandy(row, col);
            gameState.board[row][col] = candy;
        }
    }
    
    // Başlangıçta eşleşme olmamasını sağla
    while (findAllMatches().length > 0) {
        for (let row = 0; row < GAME_CONFIG.boardSize; row++) {
            for (let col = 0; col < GAME_CONFIG.boardSize; col++) {
                const matches = findMatchesAt(row, col);
                if (matches.length >= 3) {
                    gameState.board[row][col].type = getRandomCandyType();
                    updateCandyElement(row, col);
                }
            }
        }
    }
}

function createCandy(row, col) {
    const type = getRandomCandyType();
    const candy = {
        type: type,
        row: row,
        col: col,
        special: null
    };
    
    const element = document.createElement('div');
    element.className = `candy ${type}`;
    element.dataset.row = row;
    element.dataset.col = col;
    element.textContent = GAME_CONFIG.candyEmojis[type];
    element.style.animationDelay = `${(row * GAME_CONFIG.boardSize + col) * 30}ms`;
    
    element.addEventListener('click', () => handleCandyClick(row, col));
    element.addEventListener('touchstart', (e) => {
        e.preventDefault();
        handleCandyClick(row, col);
    }, { passive: false });
    
    elements.gameBoard.appendChild(element);
    
    return candy;
}

function getRandomCandyType() {
    return GAME_CONFIG.candyTypes[Math.floor(Math.random() * GAME_CONFIG.candyTypes.length)];
}

function updateCandyElement(row, col) {
    const candy = gameState.board[row][col];
    const element = getCandyElement(row, col);
    
    if (element && candy) {
        element.className = `candy ${candy.type}`;
        if (candy.special) {
            element.classList.add(candy.special);
        }
        element.textContent = GAME_CONFIG.candyEmojis[candy.type];
    }
}

function getCandyElement(row, col) {
    return elements.gameBoard.querySelector(`[data-row="${row}"][data-col="${col}"]`);
}

// ==========================================
// OYUN MEKANİKLERİ
// ==========================================
function handleCandyClick(row, col) {
    if (gameState.isAnimating) return;
    
    const clickedCandy = gameState.board[row][col];
    if (!clickedCandy) return;
    
    if (!gameState.selectedCandy) {
        // İlk seçim
        gameState.selectedCandy = { row, col };
        getCandyElement(row, col).classList.add('selected');
        playSound('select');
    } else {
        // İkinci seçim
        const firstRow = gameState.selectedCandy.row;
        const firstCol = gameState.selectedCandy.col;
        
        getCandyElement(firstRow, firstCol).classList.remove('selected');
        
        // Komşu mu kontrol et
        if (isAdjacent(firstRow, firstCol, row, col)) {
            swapCandies(firstRow, firstCol, row, col);
        } else {
            // Yeni seçim yap
            gameState.selectedCandy = { row, col };
            getCandyElement(row, col).classList.add('selected');
            playSound('select');
            return;
        }
        
        gameState.selectedCandy = null;
    }
}

function isAdjacent(row1, col1, row2, col2) {
    return (Math.abs(row1 - row2) + Math.abs(col1 - col2)) === 1;
}

async function swapCandies(row1, col1, row2, col2) {
    gameState.isAnimating = true;
    
    // Swap animation
    const element1 = getCandyElement(row1, col1);
    const element2 = getCandyElement(row2, col2);
    
    // Görsel swap
    animateSwap(element1, element2, row1, col1, row2, col2);
    
    // Veri swap
    const temp = gameState.board[row1][col1];
    gameState.board[row1][col1] = gameState.board[row2][col2];
    gameState.board[row2][col2] = temp;
    
    // Pozisyon güncelle
    gameState.board[row1][col1].row = row1;
    gameState.board[row1][col1].col = col1;
    gameState.board[row2][col2].row = row2;
    gameState.board[row2][col2].col = col2;
    
    await sleep(GAME_CONFIG.animationDuration);
    
    // Eşleşme kontrolü
    const matches = findAllMatches();
    
    if (matches.length > 0) {
        // Hamle say
        gameState.moves--;
        elements.movesLeft.textContent = gameState.moves;
        
        // Eşleşmeleri işle
        gameState.comboCount = 0;
        await processMatches();
        
        // Oyun durumu kontrolü
        checkGameStatus();
    } else {
        // Geçersiz hamle - geri al
        playSound('invalid');
        
        animateSwap(element1, element2, row1, col1, row2, col2);
        
        const temp = gameState.board[row1][col1];
        gameState.board[row1][col1] = gameState.board[row2][col2];
        gameState.board[row2][col2] = temp;
        
        gameState.board[row1][col1].row = row1;
        gameState.board[row1][col1].col = col1;
        gameState.board[row2][col2].row = row2;
        gameState.board[row2][col2].col = col2;
        
        await sleep(GAME_CONFIG.animationDuration);
    }
    
    // Element dataset güncelle
    updateBoardElements();
    
    gameState.isAnimating = false;
}

function animateSwap(element1, element2, row1, col1, row2, col2) {
    const cellSize = element1.offsetWidth + 4; // gap dahil
    
    const dx = (col2 - col1) * cellSize;
    const dy = (row2 - row1) * cellSize;
    
    element1.style.transform = `translate(${dx}px, ${dy}px)`;
    element2.style.transform = `translate(${-dx}px, ${-dy}px)`;
    
    setTimeout(() => {
        element1.style.transform = '';
        element2.style.transform = '';
    }, GAME_CONFIG.animationDuration);
}

function updateBoardElements() {
    const existingElements = Array.from(elements.gameBoard.children);
    
    existingElements.forEach(el => el.remove());
    
    for (let row = 0; row < GAME_CONFIG.boardSize; row++) {
        for (let col = 0; col < GAME_CONFIG.boardSize; col++) {
            const candy = gameState.board[row][col];
            if (candy) {
                const element = document.createElement('div');
                element.className = `candy ${candy.type}`;
                if (candy.special) {
                    element.classList.add(candy.special);
                }
                element.dataset.row = row;
                element.dataset.col = col;
                element.textContent = GAME_CONFIG.candyEmojis[candy.type];
                
                element.addEventListener('click', () => handleCandyClick(row, col));
                element.addEventListener('touchstart', (e) => {
                    e.preventDefault();
                    handleCandyClick(row, col);
                }, { passive: false });
                
                elements.gameBoard.appendChild(element);
            }
        }
    }
}

// ==========================================
// EŞLEŞMELERİ BULMA
// ==========================================
function findAllMatches() {
    const matches = new Set();
    
    // Yatay eşleşmeler
    for (let row = 0; row < GAME_CONFIG.boardSize; row++) {
        for (let col = 0; col < GAME_CONFIG.boardSize - 2; col++) {
            const match = findHorizontalMatch(row, col);
            match.forEach(pos => matches.add(`${pos.row},${pos.col}`));
        }
    }
    
    // Dikey eşleşmeler
    for (let row = 0; row < GAME_CONFIG.boardSize - 2; row++) {
        for (let col = 0; col < GAME_CONFIG.boardSize; col++) {
            const match = findVerticalMatch(row, col);
            match.forEach(pos => matches.add(`${pos.row},${pos.col}`));
        }
    }
    
    return Array.from(matches).map(pos => {
        const [row, col] = pos.split(',').map(Number);
        return { row, col };
    });
}

function findHorizontalMatch(row, startCol) {
    const candy = gameState.board[row][startCol];
    if (!candy) return [];
    
    const type = candy.type;
    const match = [{ row, col: startCol }];
    
    for (let col = startCol + 1; col < GAME_CONFIG.boardSize; col++) {
        if (gameState.board[row][col] && gameState.board[row][col].type === type) {
            match.push({ row, col });
        } else {
            break;
        }
    }
    
    return match.length >= 3 ? match : [];
}

function findVerticalMatch(startRow, col) {
    const candy = gameState.board[startRow][col];
    if (!candy) return [];
    
    const type = candy.type;
    const match = [{ row: startRow, col }];
    
    for (let row = startRow + 1; row < GAME_CONFIG.boardSize; row++) {
        if (gameState.board[row][col] && gameState.board[row][col].type === type) {
            match.push({ row, col });
        } else {
            break;
        }
    }
    
    return match.length >= 3 ? match : [];
}

function findMatchesAt(row, col) {
    const matches = new Set();
    
    // Yatay kontrol
    let left = col;
    while (left > 0 && gameState.board[row][left - 1] && 
           gameState.board[row][left - 1].type === gameState.board[row][col].type) {
        left--;
    }
    
    let right = col;
    while (right < GAME_CONFIG.boardSize - 1 && gameState.board[row][right + 1] && 
           gameState.board[row][right + 1].type === gameState.board[row][col].type) {
        right++;
    }
    
    if (right - left >= 2) {
        for (let c = left; c <= right; c++) {
            matches.add(`${row},${c}`);
        }
    }
    
    // Dikey kontrol
    let top = row;
    while (top > 0 && gameState.board[top - 1][col] && 
           gameState.board[top - 1][col].type === gameState.board[row][col].type) {
        top--;
    }
    
    let bottom = row;
    while (bottom < GAME_CONFIG.boardSize - 1 && gameState.board[bottom + 1][col] && 
           gameState.board[bottom + 1][col].type === gameState.board[row][col].type) {
        bottom++;
    }
    
    if (bottom - top >= 2) {
        for (let r = top; r <= bottom; r++) {
            matches.add(`${r},${col}`);
        }
    }
    
    return Array.from(matches).map(pos => {
        const [r, c] = pos.split(',').map(Number);
        return { row: r, col: c };
    });
}

// ==========================================
// EŞLEŞMELERİ İŞLEME
// ==========================================
async function processMatches() {
    let matches = findAllMatches();
    
    while (matches.length > 0) {
        gameState.comboCount++;
        
        // Kombo göster
        if (gameState.comboCount > 1) {
            showCombo(gameState.comboCount);
        }
        
        // Puan hesapla
        const score = calculateScore(matches);
        gameState.score += score;
        
        // Puan animasyonu göster
        showScorePopup(matches[0], score);
        
        // UI güncelle
        elements.currentScore.textContent = gameState.score;
        updateProgressBar();
        updateStarMarkers();
        
        // Eşleşen şekerleri kaldır
        await removeMatches(matches);
        
        // Şekerleri düşür
        await dropCandies();
        
        // Yeni şekerler ekle
        await fillBoard();
        
        // Tekrar kontrol et
        matches = findAllMatches();
    }
    
    // Geçerli hamle yoksa karıştır
    if (!hasValidMoves()) {
        await shuffleBoard();
    }
}

function calculateScore(matches) {
    let score = matches.length * GAME_CONFIG.baseScore;
    
    // Eşleşme bonusu
    if (matches.length >= 6) {
        score += GAME_CONFIG.matchBonus[6];
    } else if (matches.length >= 5) {
        score += GAME_CONFIG.matchBonus[5];
    } else if (matches.length >= 4) {
        score += GAME_CONFIG.matchBonus[4];
    }
    
    // Kombo çarpanı
    score *= Math.pow(GAME_CONFIG.comboMultiplier, gameState.comboCount - 1);
    
    return Math.floor(score);
}

async function removeMatches(matches) {
    playSound('match');
    
    // Animasyon
    matches.forEach(({ row, col }) => {
        const element = getCandyElement(row, col);
        if (element) {
            element.classList.add('matched');
        }
    });
    
    await sleep(400);
    
    // Kaldır
    matches.forEach(({ row, col }) => {
        const element = getCandyElement(row, col);
        if (element) {
            element.remove();
        }
        gameState.board[row][col] = null;
    });
}

async function dropCandies() {
    let dropped = false;
    
    for (let col = 0; col < GAME_CONFIG.boardSize; col++) {
        let emptySpaces = 0;
        
        for (let row = GAME_CONFIG.boardSize - 1; row >= 0; row--) {
            if (!gameState.board[row][col]) {
                emptySpaces++;
            } else if (emptySpaces > 0) {
                // Şekeri düşür
                const candy = gameState.board[row][col];
                const newRow = row + emptySpaces;
                
                gameState.board[newRow][col] = candy;
                gameState.board[row][col] = null;
                candy.row = newRow;
                
                dropped = true;
            }
        }
    }
    
    if (dropped) {
        updateBoardElements();
        await sleep(GAME_CONFIG.animationDuration);
    }
}

async function fillBoard() {
    let filled = false;
    
    for (let col = 0; col < GAME_CONFIG.boardSize; col++) {
        for (let row = 0; row < GAME_CONFIG.boardSize; row++) {
            if (!gameState.board[row][col]) {
                const type = getRandomCandyType();
                gameState.board[row][col] = {
                    type: type,
                    row: row,
                    col: col,
                    special: null
                };
                filled = true;
            }
        }
    }
    
    if (filled) {
        updateBoardElements();
        await sleep(GAME_CONFIG.animationDuration);
    }
}

// ==========================================
// YARDIMCI FONKSİYONLAR
// ==========================================
function hasValidMoves() {
    for (let row = 0; row < GAME_CONFIG.boardSize; row++) {
        for (let col = 0; col < GAME_CONFIG.boardSize; col++) {
            // Sağ komşu ile değiştir
            if (col < GAME_CONFIG.boardSize - 1) {
                swapTemp(row, col, row, col + 1);
                if (findAllMatches().length > 0) {
                    swapTemp(row, col, row, col + 1);
                    return true;
                }
                swapTemp(row, col, row, col + 1);
            }
            
            // Alt komşu ile değiştir
            if (row < GAME_CONFIG.boardSize - 1) {
                swapTemp(row, col, row + 1, col);
                if (findAllMatches().length > 0) {
                    swapTemp(row, col, row + 1, col);
                    return true;
                }
                swapTemp(row, col, row + 1, col);
            }
        }
    }
    return false;
}

function swapTemp(row1, col1, row2, col2) {
    const temp = gameState.board[row1][col1];
    gameState.board[row1][col1] = gameState.board[row2][col2];
    gameState.board[row2][col2] = temp;
}

function showHint() {
    if (gameState.isAnimating) return;
    
    // Mevcut ipuçlarını temizle
    document.querySelectorAll('.candy.hint').forEach(el => el.classList.remove('hint'));
    
    // Geçerli hamle bul
    for (let row = 0; row < GAME_CONFIG.boardSize; row++) {
        for (let col = 0; col < GAME_CONFIG.boardSize; col++) {
            // Sağ komşu ile kontrol
            if (col < GAME_CONFIG.boardSize - 1) {
                swapTemp(row, col, row, col + 1);
                if (findAllMatches().length > 0) {
                    swapTemp(row, col, row, col + 1);
                    getCandyElement(row, col)?.classList.add('hint');
                    getCandyElement(row, col + 1)?.classList.add('hint');
                    playSound('hint');
                    
                    setTimeout(() => {
                        document.querySelectorAll('.candy.hint').forEach(el => el.classList.remove('hint'));
                    }, 2000);
                    return;
                }
                swapTemp(row, col, row, col + 1);
            }
            
            // Alt komşu ile kontrol
            if (row < GAME_CONFIG.boardSize - 1) {
                swapTemp(row, col, row + 1, col);
                if (findAllMatches().length > 0) {
                    swapTemp(row, col, row + 1, col);
                    getCandyElement(row, col)?.classList.add('hint');
                    getCandyElement(row + 1, col)?.classList.add('hint');
                    playSound('hint');
                    
                    setTimeout(() => {
                        document.querySelectorAll('.candy.hint').forEach(el => el.classList.remove('hint'));
                    }, 2000);
                    return;
                }
                swapTemp(row, col, row + 1, col);
            }
        }
    }
}

async function shuffleBoard() {
    if (gameState.isAnimating) return;
    
    gameState.isAnimating = true;
    playSound('shuffle');
    
    // Tüm şekerleri karıştır
    const allCandies = [];
    for (let row = 0; row < GAME_CONFIG.boardSize; row++) {
        for (let col = 0; col < GAME_CONFIG.boardSize; col++) {
            if (gameState.board[row][col]) {
                allCandies.push(gameState.board[row][col].type);
            }
        }
    }
    
    // Fisher-Yates shuffle
    for (let i = allCandies.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [allCandies[i], allCandies[j]] = [allCandies[j], allCandies[i]];
    }
    
    // Yeniden yerleştir
    let index = 0;
    for (let row = 0; row < GAME_CONFIG.boardSize; row++) {
        for (let col = 0; col < GAME_CONFIG.boardSize; col++) {
            if (gameState.board[row][col]) {
                gameState.board[row][col].type = allCandies[index++];
            }
        }
    }
    
    // Eşleşmeleri kaldır
    while (findAllMatches().length > 0 || !hasValidMoves()) {
        for (let row = 0; row < GAME_CONFIG.boardSize; row++) {
            for (let col = 0; col < GAME_CONFIG.boardSize; col++) {
                if (gameState.board[row][col]) {
                    gameState.board[row][col].type = getRandomCandyType();
                }
            }
        }
    }
    
    updateBoardElements();
    await sleep(500);
    
    gameState.isAnimating = false;
}

// ==========================================
// İLERLEME VE PUANLAMA
// ==========================================
function updateProgressBar() {
    const progress = Math.min(100, (gameState.score / gameState.targetScore) * 100);
    elements.scoreProgress.style.width = `${progress}%`;
}

function updateStarMarkers() {
    const levelConfig = LEVELS[gameState.currentLevel - 1];
    const starMarkers = document.querySelectorAll('.star-marker');
    
    starMarkers.forEach((marker, index) => {
        const starNum = index + 1;
        const threshold = levelConfig.starThresholds[starNum];
        
        if (gameState.score >= threshold) {
            marker.classList.add('active');
            if (gameState.stars < starNum) {
                gameState.stars = starNum;
                playSound('star');
            }
        }
    });
}

function showScorePopup(position, score) {
    const element = getCandyElement(position.row, position.col);
    if (!element) return;
    
    const rect = element.getBoundingClientRect();
    const popup = document.createElement('div');
    popup.className = 'score-popup';
    popup.textContent = `+${score}`;
    popup.style.left = `${rect.left + rect.width / 2}px`;
    popup.style.top = `${rect.top}px`;
    
    document.body.appendChild(popup);
    
    setTimeout(() => popup.remove(), 1000);
}

function showCombo(count) {
    const comboTexts = ['', '', 'İYİ!', 'SÜPER!', 'HARİKA!', 'MÜTHİŞ!', 'İNANILMAZ!'];
    const text = comboTexts[Math.min(count, comboTexts.length - 1)] || 'EFSANE!';
    
    const indicator = document.createElement('div');
    indicator.className = 'combo-indicator';
    indicator.textContent = `${count}x ${text}`;
    
    document.body.appendChild(indicator);
    
    setTimeout(() => indicator.remove(), 1000);
}

// ==========================================
// OYUN DURUMU KONTROLÜ
// ==========================================
function checkGameStatus() {
    // Kazandı mı?
    if (gameState.score >= gameState.targetScore && gameState.moves >= 0) {
        setTimeout(() => showLevelComplete(), 500);
        return;
    }
    
    // Kaybetti mi?
    if (gameState.moves <= 0) {
        if (gameState.score >= gameState.targetScore) {
            setTimeout(() => showLevelComplete(), 500);
        } else {
            setTimeout(() => showLevelFailed(), 500);
        }
    }
}

function showLevelComplete() {
    playSound('win');
    
    // Yıldızları güncelle
    const earnedStars = gameState.stars || 1;
    const previousStars = gameState.levelStars[gameState.currentLevel] || 0;
    
    if (earnedStars > previousStars) {
        gameState.totalStars += (earnedStars - previousStars);
        gameState.levelStars[gameState.currentLevel] = earnedStars;
    }
    
    // Yeni seviye aç
    let newLevelUnlocked = false;
    if (gameState.currentLevel >= gameState.maxUnlockedLevel && 
        gameState.currentLevel < GAME_CONFIG.totalLevels) {
        gameState.maxUnlockedLevel = gameState.currentLevel + 1;
        newLevelUnlocked = true;
    }
    
    saveGameData();
    
    // UI güncelle
    elements.finalScore.textContent = gameState.score;
    
    // Yıldızları göster
    elements.star1.classList.toggle('earned', earnedStars >= 1);
    elements.star2.classList.toggle('earned', earnedStars >= 2);
    elements.star3.classList.toggle('earned', earnedStars >= 3);
    
    // Bonus
    const bonus = calculateBonus();
    if (bonus > 0) {
        elements.bonusSection.style.display = 'block';
        elements.bonusAmount.textContent = `+${bonus}`;
        gameState.score += bonus;
        elements.finalScore.textContent = gameState.score;
    } else {
        elements.bonusSection.style.display = 'none';
    }
    
    // Konfeti oluştur
    createConfetti();
    
    // Popup göster
    elements.levelComplete.classList.add('active');
    
    // Yeni seviye açıldı mı?
    if (newLevelUnlocked) {
        setTimeout(() => {
            showNewLevelUnlock(gameState.maxUnlockedLevel);
        }, 2000);
    }
}

function showLevelFailed() {
    playSound('lose');
    
    elements.failedScore.textContent = gameState.score;
    elements.levelFailed.classList.add('active');
}

function calculateBonus() {
    let bonus = 0;
    
    // Kalan hamle bonusu
    bonus += gameState.moves * 20;
    
    // Yıldız bonusu
    bonus += gameState.stars * 100;
    
    return bonus;
}

// ==========================================
// GÖRSEL EFEKTLER
// ==========================================
function createConfetti() {
    const container = document.querySelector('.confetti-container');
    container.innerHTML = '';
    
    const colors = ['#FF6B6B', '#4ECDC4', '#FFE66D', '#A29BFE', '#FF7F50', '#96E6A1'];
    
    for (let i = 0; i < 50; i++) {
        const confetti = document.createElement('div');
        confetti.className = 'confetti';
        confetti.style.left = `${Math.random() * 100}%`;
        confetti.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
        confetti.style.animationDelay = `${Math.random() * 2}s`;
        confetti.style.animationDuration = `${2 + Math.random() * 2}s`;
        
        container.appendChild(confetti);
    }
}

function createFloatingCandies() {
    const container = document.querySelector('.floating-candies');
    const emojis = ['🍬', '🍭', '🍫', '🍩', '🧁', '🍪', '⭐', '💎'];
    
    for (let i = 0; i < 20; i++) {
        const candy = document.createElement('div');
        candy.textContent = emojis[Math.floor(Math.random() * emojis.length)];
        candy.style.position = 'absolute';
        candy.style.fontSize = `${20 + Math.random() * 30}px`;
        candy.style.left = `${Math.random() * 100}%`;
        candy.style.top = `${Math.random() * 100}%`;
        candy.style.opacity = '0.3';
        candy.style.animation = `float ${5 + Math.random() * 5}s ease-in-out infinite`;
        candy.style.animationDelay = `${Math.random() * 5}s`;
        
        container.appendChild(candy);
    }
    
    // Float animasyonu ekle
    const style = document.createElement('style');
    style.textContent = `
        @keyframes float {
            0%, 100% { transform: translateY(0) rotate(0deg); }
            50% { transform: translateY(-20px) rotate(10deg); }
        }
    `;
    document.head.appendChild(style);
}

// ==========================================
// SES EFEKTLERİ
// ==========================================
function playSound(type) {
    if (!gameState.settings.sound) return;
    
    // Web Audio API ile basit ses efektleri
    const audioContext = new (window.AudioContext || window.webkitAudioContext)();
    const oscillator = audioContext.createOscillator();
    const gainNode = audioContext.createGain();
    
    oscillator.connect(gainNode);
    gainNode.connect(audioContext.destination);
    
    const sounds = {
        select: { freq: 400, duration: 0.1 },
        match: { freq: 600, duration: 0.2 },
        invalid: { freq: 200, duration: 0.3 },
        star: { freq: 800, duration: 0.3 },
        win: { freq: 1000, duration: 0.5 },
        lose: { freq: 150, duration: 0.5 },
        hint: { freq: 500, duration: 0.2 },
        shuffle: { freq: 350, duration: 0.4 }
    };
    
    const sound = sounds[type] || sounds.select;
    
    oscillator.frequency.value = sound.freq;
    oscillator.type = 'sine';
    
    gainNode.gain.setValueAtTime(0.1, audioContext.currentTime);
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + sound.duration);
    
    oscillator.start(audioContext.currentTime);
    oscillator.stop(audioContext.currentTime + sound.duration);
}

// ==========================================
// YARDIMCI FONKSİYONLAR
// ==========================================
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// ==========================================
// BAŞLAT
// ==========================================
document.addEventListener('DOMContentLoaded', init);
