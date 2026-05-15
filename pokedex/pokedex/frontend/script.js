const API_BASE = 'http://localhost:8080/api/pokemon';

const searchForm = document.getElementById('searchForm');
const searchInput = document.getElementById('searchInput');
const loader = document.getElementById('loader');
const errorContainer = document.getElementById('errorContainer');
const errorTitle = document.getElementById('errorTitle');
const errorMessage = document.getElementById('errorMessage');
const retryBtn = document.getElementById('retryBtn');
const pokemonSection = document.getElementById('pokemonSection');
const historySection = document.getElementById('historySection');
const historyList = document.getElementById('historyList');

const pokemonId = document.getElementById('pokemonId');
const cacheBadge = document.getElementById('cacheBadge');
const pokemonImage = document.getElementById('pokemonImage');
const shinyToggle = document.getElementById('shinyToggle');
const pokemonName = document.getElementById('pokemonName');
const typesContainer = document.getElementById('typesContainer');
const pokemonHeight = document.getElementById('pokemonHeight');
const pokemonWeight = document.getElementById('pokemonWeight');
const abilitiesContainer = document.getElementById('abilitiesContainer');
const statsContainer = document.getElementById('statsContainer');
const cardHeader = document.getElementById('cardHeader');

let currentPokemon = null;
let isShiny = false;
let lastSearch = '';

const TYPE_COLORS = {
    normal: '#a8a878', fire: '#f08030', water: '#6890f0', electric: '#f8d030',
    grass: '#78c850', ice: '#98d8d8', fighting: '#c03028', poison: '#a040a0',
    ground: '#e0c068', flying: '#a890f0', psychic: '#f85888', bug: '#a8b820',
    rock: '#b8a038', ghost: '#705898', dragon: '#7038f8', dark: '#705848',
    steel: '#b8b8d0', fairy: '#ee99ac'
};

const STAT_LABELS = {
    'hp': 'HP',
    'attack': 'Attack',
    'defense': 'Defense',
    'special-attack': 'Sp. Attack',
    'special-defense': 'Sp. Defense',
    'speed': 'Speed'
};

function showLoader() {
    loader.classList.add('active');
    errorContainer.classList.remove('active');
    pokemonSection.classList.remove('active');
}

function hideLoader() {
    loader.classList.remove('active');
}

function showError(title, message) {
    hideLoader();
    errorTitle.textContent = title;
    errorMessage.textContent = message;
    errorContainer.classList.add('active');
    pokemonSection.classList.remove('active');
}

function getStatColor(value) {
    if (value < 50) return 'low';
    if (value < 80) return 'medium';
    if (value < 120) return 'high';
    return 'max';
}

function renderPokemon(data) {
    currentPokemon = data;
    isShiny = false;

    pokemonId.textContent = '#' + String(data.id).padStart(3, '0');

    if (data.fromCache) {
        cacheBadge.classList.add('visible');
    } else {
        cacheBadge.classList.remove('visible');
    }

    const primaryType = data.types[0] || 'normal';
    const color1 = TYPE_COLORS[primaryType] || '#a8a878';
    const color2 = data.types[1] ? (TYPE_COLORS[data.types[1]] || color1) : color1;
    cardHeader.style.background = 'linear-gradient(135deg, ' + color1 + ', ' + color2 + ')';
    pokemonId.style.color = 'rgba(255,255,255,0.85)';

    pokemonImage.src = data.imageUrl || '';
    pokemonImage.alt = data.name;
    shinyToggle.textContent = '✨ Shiny';
    shinyToggle.style.background = '';

    pokemonName.textContent = data.name;

    typesContainer.innerHTML = '';
    data.types.forEach(function(type) {
        const badge = document.createElement('span');
        badge.className = 'type-badge type-' + type;
        badge.textContent = type;
        typesContainer.appendChild(badge);
    });

    pokemonHeight.textContent = (data.height / 10).toFixed(1) + ' m';
    pokemonWeight.textContent = (data.weight / 10).toFixed(1) + ' kg';

    abilitiesContainer.innerHTML = '';
    data.abilities.forEach(function(ability) {
        const badge = document.createElement('span');
        badge.className = 'ability-badge';
        badge.textContent = ability;
        abilitiesContainer.appendChild(badge);
    });

    statsContainer.innerHTML = '';
    data.stats.forEach(function(stat) {
        const label = STAT_LABELS[stat.name] || stat.name;
        const percentage = Math.min((stat.value / 255) * 100, 100);
        const colorClass = getStatColor(stat.value);

        const row = document.createElement('div');
        row.className = 'stat-row';
        row.innerHTML =
            '<span class="stat-name">' + label + '</span>' +
            '<span class="stat-value">' + stat.value + '</span>' +
            '<div class="stat-bar-bg">' +
                '<div class="stat-bar-fill ' + colorClass + '" data-width="' + percentage + '"></div>' +
            '</div>';
        statsContainer.appendChild(row);
    });

    pokemonSection.classList.add('active');

    requestAnimationFrame(function() {
        document.querySelectorAll('.stat-bar-fill').forEach(function(bar) {
            bar.style.width = bar.dataset.width + '%';
        });
    });
}

async function searchPokemon(name) {
    const trimmed = name.trim().toLowerCase();

    if (trimmed === '') {
        showError('Empty Search', 'Please type a Pokémon name to search.');
        return;
    }

    lastSearch = trimmed;
    showLoader();

    try {
        const response = await fetch(API_BASE + '/' + encodeURIComponent(trimmed));
        const data = await response.json();

        if (!response.ok) {
            if (response.status === 404) {
                showError('Not Found', '"' + trimmed + '" does not exist. Check the spelling and try again.');
            } else if (response.status === 503) {
                showError('API Unavailable', 'Could not reach PokéAPI. Check your internet connection.');
            } else {
                showError('Error', data.message || 'Something went wrong.');
            }
            return;
        }

        hideLoader();
        errorContainer.classList.remove('active');
        renderPokemon(data);
        loadHistory();

    } catch (err) {
        showError('Connection Error', 'Cannot connect to the backend. Make sure Spring Boot is running on port 8080.');
    }
}

shinyToggle.addEventListener('click', function() {
    if (!currentPokemon) return;
    isShiny = !isShiny;
    if (isShiny) {
        pokemonImage.src = currentPokemon.shinyImageUrl || currentPokemon.imageUrl;
        shinyToggle.textContent = '🌟 Normal';
        shinyToggle.style.background = 'linear-gradient(135deg, #f4d03f, #f39c12)';
    } else {
        pokemonImage.src = currentPokemon.imageUrl;
        shinyToggle.textContent = '✨ Shiny';
        shinyToggle.style.background = '';
    }
});

searchForm.addEventListener('submit', function(e) {
    e.preventDefault();
    searchPokemon(searchInput.value);
});

retryBtn.addEventListener('click', function() {
    if (lastSearch) {
        searchInput.value = lastSearch;
        searchPokemon(lastSearch);
    } else {
        errorContainer.classList.remove('active');
    }
});

document.querySelectorAll('.quick-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
        searchInput.value = btn.dataset.name;
        searchPokemon(btn.dataset.name);
    });
});

async function loadHistory() {
    try {
        const response = await fetch(API_BASE + '/history/recent');
        if (!response.ok) return;

        const history = await response.json();
        if (!history || history.length === 0) {
            historySection.classList.remove('active');
            return;
        }

        historyList.innerHTML = '';
        history.forEach(function(item) {
            const btn = document.createElement('button');
            btn.className = 'history-item';
            btn.textContent = item.searchTerm;
            btn.addEventListener('click', function() {
                searchInput.value = item.searchTerm;
                searchPokemon(item.searchTerm);
            });
            historyList.appendChild(btn);
        });

        historySection.classList.add('active');
    } catch (err) {
        historySection.classList.remove('active');
    }
}

loadHistory();
