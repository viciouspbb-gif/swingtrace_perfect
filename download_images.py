"""
著作権フリー画像収集スクリプト
CC0ライセンスの画像をPixabay, Unsplash, Pexelsから収集
"""

import os
import requests
import time
from pathlib import Path

class ImageDownloader:
    def __init__(self, output_dir="downloaded_images"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)
        
        # APIキー（環境変数から取得）
        self.pixabay_key = os.getenv("PIXABAY_API_KEY", "")
        self.unsplash_key = os.getenv("UNSPLASH_ACCESS_KEY", "")
        self.pexels_key = os.getenv("PEXELS_API_KEY", "")
        
        print(f"📁 出力ディレクトリ: {self.output_dir}")
    
    def download_from_pixabay(self, query, max_images=50):
        """
        Pixabayから画像をダウンロード
        APIキー取得: https://pixabay.com/api/docs/
        """
        if not self.pixabay_key:
            print("⚠️ PIXABAY_API_KEYが設定されていません")
            print("   https://pixabay.com/api/docs/ でAPIキーを取得してください")
            return 0
        
        print(f"\n🔍 Pixabayから検索: '{query}'")
        
        url = "https://pixabay.com/api/"
        params = {
            "key": self.pixabay_key,
            "q": query,
            "image_type": "photo",
            "per_page": min(max_images, 200),
            "safesearch": "true"
        }
        
        try:
            response = requests.get(url, params=params)
            response.raise_for_status()
            data = response.json()
            
            hits = data.get("hits", [])
            print(f"✅ {len(hits)}件見つかりました")
            
            downloaded = 0
            for i, hit in enumerate(hits[:max_images]):
                image_url = hit.get("largeImageURL") or hit.get("webformatURL")
                if image_url:
                    filename = f"pixabay_{query.replace(' ', '_')}_{i+1:03d}.jpg"
                    if self._download_image(image_url, filename):
                        downloaded += 1
                    time.sleep(0.5)  # レート制限対策
            
            print(f"✅ Pixabay: {downloaded}枚ダウンロード完了")
            return downloaded
            
        except Exception as e:
            print(f"❌ Pixabayエラー: {e}")
            return 0
    
    def download_from_unsplash(self, query, max_images=50):
        """
        Unsplashから画像をダウンロード
        APIキー取得: https://unsplash.com/developers
        """
        if not self.unsplash_key:
            print("⚠️ UNSPLASH_ACCESS_KEYが設定されていません")
            print("   https://unsplash.com/developers でAPIキーを取得してください")
            return 0
        
        print(f"\n🔍 Unsplashから検索: '{query}'")
        
        url = "https://api.unsplash.com/search/photos"
        headers = {"Authorization": f"Client-ID {self.unsplash_key}"}
        params = {
            "query": query,
            "per_page": min(max_images, 30),
            "orientation": "landscape"
        }
        
        try:
            response = requests.get(url, headers=headers, params=params)
            response.raise_for_status()
            data = response.json()
            
            results = data.get("results", [])
            print(f"✅ {len(results)}件見つかりました")
            
            downloaded = 0
            for i, result in enumerate(results[:max_images]):
                image_url = result.get("urls", {}).get("regular")
                if image_url:
                    filename = f"unsplash_{query.replace(' ', '_')}_{i+1:03d}.jpg"
                    if self._download_image(image_url, filename):
                        downloaded += 1
                    time.sleep(0.5)
            
            print(f"✅ Unsplash: {downloaded}枚ダウンロード完了")
            return downloaded
            
        except Exception as e:
            print(f"❌ Unsplashエラー: {e}")
            return 0
    
    def download_from_pexels(self, query, max_images=50):
        """
        Pexelsから画像をダウンロード
        APIキー取得: https://www.pexels.com/api/
        """
        if not self.pexels_key:
            print("⚠️ PEXELS_API_KEYが設定されていません")
            print("   https://www.pexels.com/api/ でAPIキーを取得してください")
            return 0
        
        print(f"\n🔍 Pexelsから検索: '{query}'")
        
        url = "https://api.pexels.com/v1/search"
        headers = {"Authorization": self.pexels_key}
        params = {
            "query": query,
            "per_page": min(max_images, 80),
            "orientation": "landscape"
        }
        
        try:
            response = requests.get(url, headers=headers, params=params)
            response.raise_for_status()
            data = response.json()
            
            photos = data.get("photos", [])
            print(f"✅ {len(photos)}件見つかりました")
            
            downloaded = 0
            for i, photo in enumerate(photos[:max_images]):
                image_url = photo.get("src", {}).get("large")
                if image_url:
                    filename = f"pexels_{query.replace(' ', '_')}_{i+1:03d}.jpg"
                    if self._download_image(image_url, filename):
                        downloaded += 1
                    time.sleep(0.5)
            
            print(f"✅ Pexels: {downloaded}枚ダウンロード完了")
            return downloaded
            
        except Exception as e:
            print(f"❌ Pexelsエラー: {e}")
            return 0
    
    def _download_image(self, url, filename):
        """画像をダウンロード"""
        try:
            filepath = self.output_dir / filename
            
            # 既にダウンロード済みならスキップ
            if filepath.exists():
                print(f"⏭️  スキップ: {filename} (既存)")
                return False
            
            response = requests.get(url, timeout=10)
            response.raise_for_status()
            
            with open(filepath, "wb") as f:
                f.write(response.content)
            
            print(f"✅ ダウンロード: {filename}")
            return True
            
        except Exception as e:
            print(f"❌ エラー: {filename} - {e}")
            return False
    
    def download_all(self, queries, images_per_query=30):
        """
        複数のキーワードで画像を収集
        
        Args:
            queries: 検索キーワードのリスト
            images_per_query: 各キーワードあたりの画像数
        """
        total_downloaded = 0
        
        for query in queries:
            print(f"\n{'='*60}")
            print(f"🔍 検索キーワード: '{query}'")
            print(f"{'='*60}")
            
            # Pixabayのみから画像をダウンロード
            count = 0
            count += self.download_from_pixabay(query, images_per_query)
            
            # Unsplash と Pexels は試すが、失敗してもOK
            if self.unsplash_key:
                count += self.download_from_unsplash(query, images_per_query // 3)
            if self.pexels_key:
                count += self.download_from_pexels(query, images_per_query // 3)
            
            total_downloaded += count
            print(f"\n📊 '{query}': {count}枚ダウンロード")
        
        return total_downloaded


def main():
    """メイン処理"""
    print("="*60)
    print("🎯 著作権フリー画像収集スクリプト（Pixabay版）")
    print("="*60)
    print("\n📝 使用方法:")
    print("1. PIXABAY_API_KEYを環境変数に設定してください")
    print("   設定方法: setx PIXABAY_API_KEY \"your_key_here\"")
    print("\n2. APIキーの取得先:")
    print("   - Pixabay: https://pixabay.com/api/docs/")
    print("\n💡 オプション（追加で設定すると画像が増えます）:")
    print("   - UNSPLASH_ACCESS_KEY")
    print("   - PEXELS_API_KEY")
    print("\n" + "="*60)
    
    # ダウンローダーを初期化
    downloader = ImageDownloader("downloaded_images")
    
    # 検索キーワード
    queries = [
        "golf club head",
        "golf driver closeup",
        "golf iron club",
        "golf swing impact",
        "golf club detail",
        "golf equipment closeup"
    ]
    
    print(f"\n🎯 検索キーワード: {len(queries)}個")
    for i, q in enumerate(queries, 1):
        print(f"   {i}. {q}")
    
    # ダウンロード開始
    print(f"\n{'='*60}")
    print("🚀 ダウンロード開始")
    print(f"{'='*60}")
    
    total = downloader.download_all(queries, images_per_query=30)
    
    # 結果サマリー
    print(f"\n{'='*60}")
    print("📊 ダウンロード完了")
    print(f"{'='*60}")
    print(f"✅ 合計: {total}枚")
    print(f"📁 保存先: {downloader.output_dir}")
    print(f"\n💡 次のステップ:")
    print("1. Roboflowにアップロードしてラベル付け")
    print("2. YOLOv8形式でエクスポート")
    print("3. dataset/images/ と dataset/labels/ に配置")
    print("4. run_auto_training.bat を実行")


if __name__ == "__main__":
    main()
