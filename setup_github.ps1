<#PSScriptInfo
.VERSION 1.0.0
.GUID a1b2c3d4-e5f6-7890-abcd-ef1234567890
.AUTHOR Maitre Kobayashi
.COMPANYNAME 
.COPYRIGHT (C) 2026 Maitre Kobayashi
.TAGS GitHub, Setup, KobaAntiScam
.LICENSEURI 
.PROJECTURI 
.ICONURI 
.EXTERNALMODULEDEPENDENCIES 
.REQUIREDSCRIPTS 
.EXTERNALSCRIPTDEPENDENCIES 
.RELEASENOTES
#>

<#
.FICHIER : setup_github.ps1
.CHEMIN : D:/Kobayashi/Developpement/shield/setup_github.ps1
.LIGNES : ~100
.RÔLE : Script de configuration GitHub
.DATE : 2026-10-14
.VERSION : 1.0.0
.ORIGINE : Création
.AUTEUR : Maitre Kobayashi
.STATUT : STABLE
.DÉPENDANCES : Aucun (nécessite GitHub CLI ou API)
.LIEN : 
.NOTES : Script pour créer le repo GitHub et pusher le code
#>

<#
SCRIPT POUR CRÉER LE REPO GITHUB PRIVÉ ET PUSHER LE CODE
========================================================

Ce script te guide pour créer le dépôt GitHub privé MaitreKoba/KobaAntiScam
et pusher le code initial.

MÉTHODE 1: Utilisation de GitHub CLI (recommandé)
MÉTHODE 2: Utilisation de l'API GitHub avec un token
MÉTHODE 3: Création manuelle via le site web

#>

param(
    [string]$RepoName = "KobaAntiScam",
    [string]$Owner = "MaitreKoba",
    [string]$Description = "KobaAntiScam - Application Android de filtrage d'appels et SMS contre les arnaques",
    [switch]$Private = $true,
    [switch]$Help = $false
)

if ($Help) {
    Write-Host ""`n"SCRIPT DE CONFIGURATION GITHUB" -ForegroundColor Cyan
    Write-Host "=================================" -ForegroundColor Cyan
    Write-Host ""`n
    Write-Host "USAGE:" -ForegroundColor Yellow
    Write-Host "  .\setup_github.ps1 [options]"`n
    Write-Host "OPTIONS:" -ForegroundColor Yellow
    Write-Host "  -RepoName     Nom du dépôt (défaut: KobaAntiScam)"
    Write-Host "  -Owner        Propriétaire du dépôt (défaut: MaitreKoba)"
    Write-Host "  -Description  Description du dépôt"
    Write-Host "  -Private      Créer un dépôt privé (défaut: vrai)"
    Write-Host "  -Help         Affiche cette aide"`n
    Write-Host "EXEMPLE:" -ForegroundColor Yellow
    Write-Host "  .\setup_github.ps1 -RepoName KobaAntiScam -Owner MaitreKoba"`n
    exit 0
}

# Vérifier que git est installé
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Git n'est pas installé ou n'est pas dans le PATH" -ForegroundColor Red
    Write-Host "   Installe Git depuis: https://git-scm.com/download/win" -ForegroundColor Yellow
    exit 1
}

# Vérifier que l'on est dans le bon dossier
$currentDir = Get-Location
if ($currentDir -ne "D:\Kobayashi\Developpement\shield") {
    Write-Host "⚠️  Ce script doit être exécuté depuis D:\Kobayashi\Developpement\shield" -ForegroundColor Yellow
    Write-Host "   Changement de répertoire..." -ForegroundColor Gray
    Set-Location "D:\Kobayashi\Developpement\shield"
}

Write-Host ""`n
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  CRÉATION DU DÉPÔT GITHUB POUR KOBAANTISCAM                 ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""`n

# Afficher les informations
Write-Host "Nom du dépôt: $RepoName" -ForegroundColor Green
Write-Host "Propriétaire: $Owner" -ForegroundColor Green
Write-Host "Privé: $Private" -ForegroundColor Green
Write-Host "Description: $Description" -ForegroundColor Green
Write-Host ""`n

# Vérifier que le repo n'existe pas déjà
Write-Host "Vérification du dépôt existant..." -ForegroundColor Yellow
$repoUrl = "https://github.com/$Owner/$RepoName"

# Méthode 1: Vérifier avec gh CLI
if (Get-Command gh -ErrorAction SilentlyContinue) {
    Write-Host "GitHub CLI (gh) est installé" -ForegroundColor Green
    
    # Vérifier si le repo existe
    $existingRepo = gh repo view "$Owner/$RepoName" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Le dépôt $Owner/$RepoName existe déjà" -ForegroundColor Green
        $createRepo = $false
    } else {
        Write-Host "ℹ️  Le dépôt n'existe pas encore" -ForegroundColor Yellow
        $createRepo = $true
    }
    
    if ($createRepo) {
        Write-Host ""`n
        Write-Host "Création du dépôt privé $Owner/$RepoName..." -ForegroundColor Cyan
        
        # Créer le repo
        gh repo create "$RepoName" --private --description "$Description" --clone false
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Dépôt créé avec succès !" -ForegroundColor Green
        } else {
            Write-Host "❌ Échec de la création du dépôt" -ForegroundColor Red
            exit 1
        }
        
        # Ajouter le remote
        git remote add origin "https://github.com/$Owner/$RepoName.git"
        Write-Host "✅ Remote ajouté: origin" -ForegroundColor Green
        
        # Pousser le code
        Write-Host ""`n
        Write-Host "Pousser le code vers GitHub..." -ForegroundColor Cyan
        git branch -M main
        git push -u origin main
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Code poussé avec succès !" -ForegroundColor Green
        } else {
            Write-Host "❌ Échec du push" -ForegroundColor Red
            exit 1
        }
    } else {
        # Le repo existe déjà, ajouter le remote et pusher
        Write-Host ""`n
        Write-Host "Ajout du remote existant..." -ForegroundColor Cyan
        git remote add origin "https://github.com/$Owner/$RepoName.git"
        Write-Host "✅ Remote ajouté: origin" -ForegroundColor Green
        
        Write-Host ""`n
        Write-Host "Pousser le code vers GitHub..." -ForegroundColor Cyan
        git branch -M main
        git push -u origin main
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Code poussé avec succès !" -ForegroundColor Green
        } else {
            Write-Host "❌ Échec du push" -ForegroundColor Red
            Write-Host ""`n
            Write-Host "Essaie avec: git push -u origin main --force" -ForegroundColor Yellow
            exit 1
        }
    }
    
    # Afficher les infos finales
    Write-Host ""`n
    Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║  DÉPÔT GITHUB CONFIGURÉ AVEC SUCCÈS                      ║" -ForegroundColor Green
    Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""`n
    Write-Host "URL du dépôt: $repoUrl" -ForegroundColor Cyan
    Write-Host "Pour cloner: git clone $repoUrl.git" -ForegroundColor Gray
    Write-Host ""`n
    exit 0

} else {
    # Méthode sans gh CLI
    Write-Host "GitHub CLI (gh) n'est pas installé" -ForegroundColor Yellow
    Write-Host ""`n
    
    # Vérifier avec curl (API GitHub)
    if ($env:GITHUB_TOKEN) {
        Write-Host "Token GitHub trouvé dans l'environnement" -ForegroundColor Green
        $headers = @{
            "Authorization" = "token $env:GITHUB_TOKEN"
            "Accept" = "application/vnd.github.v3+json"
        }
        
        $repoExists = Invoke-RestMethod -Uri "https://api.github.com/repos/$Owner/$RepoName" -Headers $headers -Method Get -ErrorAction SilentlyContinue
        
        if ($repoExists) {
            Write-Host "✅ Le dépôt $Owner/$RepoName existe déjà" -ForegroundColor Green
            $createRepo = $false
        } else {
            Write-Host "ℹ️  Le dépôt n'existe pas encore" -ForegroundColor Yellow
            $createRepo = $true
        }
        
        if ($createRepo) {
            $body = @{
                name = $RepoName
                description = $Description
                private = $Private
                auto_init = $false
            } | ConvertTo-Json
            
            Write-Host ""`n
            Write-Host "Création du dépôt privé $Owner/$RepoName..." -ForegroundColor Cyan
            $newRepo = Invoke-RestMethod -Uri "https://api.github.com/user/repos" -Headers $headers -Method Post -Body $body
            
            if ($newRepo) {
                Write-Host "✅ Dépôt créé avec succès !" -ForegroundColor Green
                Write-Host "URL: $($newRepo.html_url)" -ForegroundColor Cyan
            }
            
            # Ajouter le remote et pusher
            git remote add origin "https://github.com/$Owner/$RepoName.git"
            Write-Host "✅ Remote ajouté: origin" -ForegroundColor Green
            
            Write-Host ""`n
            Write-Host "Pousser le code vers GitHub..." -ForegroundColor Cyan
            git branch -M main
            git push -u origin main
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✅ Code poussé avec succès !" -ForegroundColor Green
            }
        } else {
            git remote add origin "https://github.com/$Owner/$RepoName.git"
            Write-Host "✅ Remote ajouté: origin" -ForegroundColor Green
            
            Write-Host ""`n
            Write-Host "Pousser le code vers GitHub..." -ForegroundColor Cyan
            git branch -M main
            git push -u origin main
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✅ Code poussé avec succès !" -ForegroundColor Green
            }
        }
        
        Write-Host ""`n
        Write-Host "URL du dépôt: https://github.com/$Owner/$RepoName" -ForegroundColor Cyan
        exit 0
    } else {
        # Méthode manuelle
        Write-Host ""`n
        Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Yellow
        Write-Host "║  MÉTHODE MANUELLE (via le site GitHub)                   ║" -ForegroundColor Yellow
        Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Yellow
        Write-Host ""`n
        Write-Host "SUIS CES ÉTAPES:" -ForegroundColor Green
        Write-Host ""`n
        Write-Host "1. Ouvre ton navigateur: https://github.com/new" -ForegroundColor Cyan
        Write-Host ""`n
        Write-Host "2. Remplis le formulaire:" -ForegroundColor White
        Write-Host "   - Owner: $Owner" -ForegroundColor Gray
        Write-Host "   - Repository name: $RepoName" -ForegroundColor Gray
        Write-Host "   - Description: $Description" -ForegroundColor Gray
        Write-Host "   - ✅ Private (coché)" -ForegroundColor Gray
        Write-Host "   - ❌ Initialize with README (décoché)" -ForegroundColor Gray
        Write-Host "   - ❌ Add .gitignore (décoché)" -ForegroundColor Gray
        Write-Host "   - ❌ Add a license (décoché)" -ForegroundColor Gray
        Write-Host ""`n
        Write-Host "3. Clique sur 'Create repository'" -ForegroundColor White
        Write-Host ""`n
        Write-Host "4. Après la création, exécute ces commandes:" -ForegroundColor White
        Write-Host "" -ForegroundColor Gray
        Write-Host "   git remote add origin https://github.com/$Owner/$RepoName.git" -ForegroundColor Green
        Write-Host "   git branch -M main" -ForegroundColor Green
        Write-Host "   git push -u origin main" -ForegroundColor Green
        Write-Host ""`n
        Write-Host "5. Si git demande un login:" -ForegroundColor White
        Write-Host "   - Utilise un token GitHub (créé depuis Settings > Developer settings > Personal access tokens)" -ForegroundColor Yellow
        Write-Host "   - Ou utilise GitHub Desktop" -ForegroundColor Yellow
        Write-Host ""`n
        
        # Afficher les commandes à copier
        Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Gray
        Write-Host "COMMANDES À COPIER-COLLER (après création du repo):" -ForegroundColor Yellow
        Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Gray
        Write-Host ""`n
        Write-Host "cd D:\Kobayashi\Developpement\shield" -ForegroundColor Green
        Write-Host "git remote add origin https://github.com/$Owner/$RepoName.git" -ForegroundColor Green
        Write-Host "git branch -M main" -ForegroundColor Green
        Write-Host "git push -u origin main" -ForegroundColor Green
        Write-Host ""`n
        Write-Host "URL du dépôt: https://github.com/$Owner/$RepoName" -ForegroundColor Cyan
        Write-Host ""`n
        
        # Proposer d'ouvrir le navigateur
        $openBrowser = Read-Host "Veux-tu que j'ouvre le site GitHub pour créer le dépôt ? (o/n)"
        if ($openBrowser -eq "o" -or $openBrowser -eq "O" -or $openBrowser -eq "oui" -or $openBrowser -eq "OUI") {
            Start-Process "https://github.com/new"
            Write-Host ""`n
            Write-Host "Remplis le formulaire avec:" -ForegroundColor White
            Write-Host "  Repository name: $RepoName" -ForegroundColor Green
            Write-Host "  Owner: $Owner" -ForegroundColor Green
            Write-Host "  Private: ✅" -ForegroundColor Green
            Write-Host ""`n
            Write-Host "Puis exécute les commandes ci-dessus pour pusher le code" -ForegroundColor Yellow
        }
        
        exit 0
    }
}
